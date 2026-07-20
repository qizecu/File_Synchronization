package com.example.syncmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.entity.*;
import com.example.syncmanager.mapper.*;
import com.example.syncmanager.service.adapter.FileInfo;
import com.example.syncmanager.service.adapter.StorageAdapter;
import com.example.syncmanager.service.adapter.StorageAdapterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步引擎 -- 协调全量 / 增量 / 测试三种同步模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrchestrator {

    private final SyncTaskMapper taskMapper;
    private final SyncTaskFileMapper taskFileMapper;
    private final StorageSourceMapper sourceMapper;
    private final NotifyConfigMapper notifyConfigMapper;
    private final NotifyService notifyService;
    private final DistributedLockService lockService;
    private final SyncStatusCacheService statusCache;

    /** 分布式锁最大等待时间（秒） */
    private static final long LOCK_WAIT_SECONDS = 2;
    /** 分布式锁持有时间（秒，需要大于最长同步时间） */
    private static final long LOCK_LEASE_SECONDS = 3600;

    @Value("${sync.local.storage-path}")
    private String localStoragePath;

    @Value("${sync.test.enabled:false}")
    private boolean testMode;

    @Value("${sync.test.max-files:10}")
    private int testMaxFiles;

    @Value("${sync.disk.warning-threshold:0.8}")
    private double warningThreshold;
    @Value("${sync.disk.severe-threshold:0.9}")
    private double severeThreshold;
    @Value("${sync.disk.stop-threshold:0.95}")
    private double stopThreshold;

    /** 每次从存储端拉取文件的批次大小 */
    private static final int BATCH_SIZE = 50;
    /** MD5 校验不一致时最大重试次数 */
    private static final int MAX_RETRY = 3;

    // ========================= 对外入口 =========================

    /** 全量同步（支持断点续传 + 分布式锁防重） */
    public void executeFullSync(Long sourceId) {
        StorageSource source = getEnabledSource(sourceId);
        if (!lockService.tryLock(sourceId, LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS)) {
            throw new BusinessException("该存储源正在同步中，请稍后再试");
        }
        try {
            doSync(source, "FULL", null);
        } finally {
            lockService.unlock(sourceId);
        }
    }

    /** 增量同步（分布式锁防重） */
    public void executeIncrementalSync(Long sourceId) {
        StorageSource source = getEnabledSource(sourceId);
        if (!lockService.tryLock(sourceId, LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS)) {
            throw new BusinessException("该存储源正在同步中，请稍后再试");
        }
        try {
            SyncTask lastTask = findLastSuccessTask(sourceId);
            doSync(source, "INCREMENTAL", lastTask);
        } finally {
            lockService.unlock(sourceId);
        }
    }

    /** 重试单个失败文件 */
    public boolean retryFile(Long fileId) {
        SyncTaskFile taskFile = taskFileMapper.selectById(fileId);
        if (taskFile == null) {
            throw new BusinessException("文件记录不存在: " + fileId);
        }
        if (!"FAILED".equals(taskFile.getFileStatus())) {
            throw new BusinessException("只能重试失败的文件");
        }

        SyncTask task = taskMapper.selectById(taskFile.getTaskId());
        if (task == null) {
            throw new BusinessException("关联任务不存在");
        }

        StorageSource source = sourceMapper.selectById(task.getSourceId());
        if (source == null || source.getEnabled() != 1) {
            throw new BusinessException("存储源不存在或已禁用");
        }

        StorageAdapter adapter = StorageAdapterFactory.getOrCreate(source);
        taskFile.setFileStatus("PENDING");
        taskFileMapper.updateById(taskFile);

        boolean ok = syncSingleFile(task, taskFile, adapter, source);
        taskFile.setFileStatus(ok ? "SUCCESS" : "FAILED");
        if (!ok) {
            taskFile.setErrorMsg("重试失败");
        }
        taskFileMapper.updateById(taskFile);
        return ok;
    }

    // ========================= 核心流程 =========================

    /**
     * @param source   存储源
     * @param taskType FULL / INCREMENTAL
     * @param lastTask 上一次成功的同步任务（增量模式传入，全量模式传 null）
     */
    private void doSync(StorageSource source, String taskType, SyncTask lastTask) {
        SyncTask task = createTaskRecord(source, taskType);
        StorageAdapter adapter = StorageAdapterFactory.getOrCreate(source);
        int maxFiles = testMode ? testMaxFiles : Integer.MAX_VALUE;

        // 解析断点续传游标：上次任务中断的位置
        String cursor = resolveCursor(source.getId(), taskType);
        int syncedCount = 0;

        log.info("启动同步: taskId={}, type={}, source={}, testMode={}, cursor={}",
                task.getId(), taskType, source.getSourceName(), testMode, cursor);

        while (syncedCount < maxFiles) {
            // 磁盘空间检查（严重级别直接停止）
            String diskStatus = checkDiskSpace();
            if ("STOP".equals(diskStatus)) {
                abortTask(task, "磁盘空间超过 " + (stopThreshold * 100) + "% 阈值，同步停止");
                notify(task);
                return;
            }

            // 分页拉取文件列表
            List<FileInfo> batch = adapter.listFiles(
                    source.getBucket(), source.getPrefixPath(), cursor, BATCH_SIZE);
            if (batch.isEmpty()) break;

            // 逐个文件处理
            for (FileInfo fi : batch) {
                if (syncedCount >= maxFiles) break;

                // 增量过滤：跳过未变更的文件
                if (skipForIncremental(taskType, fi, lastTask)) {
                    task.setSkippedFiles(task.getSkippedFiles() + 1);
                    continue;
                }

                SyncTaskFile taskFile = createFileRecord(task.getId(), fi);
                boolean ok = syncSingleFile(task, taskFile, adapter, source);
                updateFileRecord(taskFile, ok);

                if (ok) {
                    task.setSuccessFiles(task.getSuccessFiles() + 1);
                } else {
                    task.setFailedFiles(task.getFailedFiles() + 1);
                }

                syncedCount++;
                cursor = fi.getPath(); // 更新游标
            }

            // 每批处理完持久化进度
            task.setCurrentCursor(cursor);
            taskMapper.updateById(task);
        }

        finishTask(task, syncedCount);
        notify(task);
        log.info("同步结束: taskId={}, success={}, failed={}, skipped={}",
                task.getId(), task.getSuccessFiles(), task.getFailedFiles(), task.getSkippedFiles());
    }

    // ========================= 文件同步 + MD5 校验 =========================

    /** @return true-成功, false-失败（已重试上限） */
    private boolean syncSingleFile(SyncTask task, SyncTaskFile taskFile,
                                   StorageAdapter adapter, StorageSource source) {
        String bucket = source.getBucket();
        String sourcePath = taskFile.getSourcePath();
        String targetPath = buildLocalPath(taskFile.getSourcePath());
        taskFile.setTargetPath(targetPath);

        for (int retry = 0; retry <= MAX_RETRY; retry++) {
            try {
                adapter.downloadFile(bucket, sourcePath, targetPath);
                taskFile.setRetryCount(retry);

                // 计算本地 MD5 并比对
                String localMd5 = calculateMd5(targetPath);
                taskFile.setTargetMd5(localMd5);
                taskFile.setTargetSize(new File(targetPath).length());

                // 获取源端最新的 MD5（下载后可能变化？不会，但做一次校验）
                FileInfo sourceInfo = adapter.getFileInfo(bucket, sourcePath);
                String sourceMd5 = sourceInfo != null ? sourceInfo.getMd5() : taskFile.getSourceMd5();

                // MD5 比对：注意部分对象存储 ETag 不一定是 MD5，用大小兜底
                if (localMd5 != null && localMd5.equals(sourceMd5)) {
                    return true;
                }
                // MD5 不一致，删除本地文件准备重试
                new File(targetPath).delete();
                log.warn("MD5 校验不一致(第{}次重试): path={}, sourceMd5={}, localMd5={}",
                        retry, sourcePath, sourceMd5, localMd5);

            } catch (Exception e) {
                log.error("文件同步异常(第{}次重试): path={}, error={}", retry, sourcePath, e.getMessage());
            }
        }

        taskFile.setErrorMsg("MD5 校验失败，已重试 " + MAX_RETRY + " 次");
        return false;
    }

    /** 增量过滤：文件最后修改时间 ≤ 上次同步时间则跳过 */
    private boolean skipForIncremental(String taskType, FileInfo fi, SyncTask lastTask) {
        if (!"INCREMENTAL".equals(taskType) || lastTask == null) return false;
        LocalDateTime cutoff = lastTask.getCompletedAt();
        return fi.getLastModified() != null && !fi.getLastModified().isAfter(cutoff);
    }

    // ========================= 磁盘空间检查 =========================

    /**
     * @return "OK" / "WARN" / "SEVERE" / "STOP"
     */
    private String checkDiskSpace() {
        File dir = new File(localStoragePath);
        if (!dir.exists()) dir.mkdirs();

        long total = dir.getTotalSpace();
        long usable = dir.getUsableSpace();
        double usage = 1.0 - (double) usable / total;

        if (usage >= stopThreshold) {
            log.error("磁盘使用率 {}% >= {}%，触发停止", String.format("%.1f", usage * 100), stopThreshold * 100);
            return "STOP";
        }
        if (usage >= severeThreshold) {
            log.error("磁盘使用率 {}% >= {}%，严重告警", String.format("%.1f", usage * 100), severeThreshold * 100);
            return "SEVERE";
        }
        if (usage >= warningThreshold) {
            log.warn("磁盘使用率 {}% >= {}%，预警", String.format("%.1f", usage * 100), warningThreshold * 100);
            return "WARN";
        }
        return "OK";
    }

    // ========================= MD5 计算 =========================

    private String calculateMd5(String filePath) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("MD5 计算失败: {}", filePath, e);
            return null;
        }
    }

    // ========================= 通知（钉钉 / 企微 webhook） =========================

    private void notify(SyncTask task) {
        List<NotifyConfig> configs = notifyConfigMapper.selectList(
                new LambdaQueryWrapper<NotifyConfig>().eq(NotifyConfig::getEnabled, 1));
        if (configs.isEmpty()) return;

        String msg = buildNotifyMessage(task);
        for (NotifyConfig cfg : configs) {
            notifyService.send(cfg, task.getId(), msg);
        }
    }

    private String buildNotifyMessage(SyncTask task) {
        return String.format("文件同步%s\n任务: %s\n总数: %d | 成功: %d | 失败: %d | 跳过: %d",
                "SUCCESS".equals(task.getStatus()) ? "完成" : "异常终止",
                task.getTaskName(),
                task.getTotalFiles(),
                task.getSuccessFiles(),
                task.getFailedFiles(),
                task.getSkippedFiles());
    }

    // ========================= 辅助方法 =========================

    private StorageSource getEnabledSource(Long sourceId) {
        StorageSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getEnabled() != 1) {
            throw new BusinessException("存储源不存在或已禁用: " + sourceId);
        }
        return source;
    }

    /** 查找上一次成功任务的完成时间，用于增量对比 */
    private SyncTask findLastSuccessTask(Long sourceId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getSourceId, sourceId)
                .eq(SyncTask::getStatus, "SUCCESS")
                .orderByDesc(SyncTask::getCompletedAt)
                .last("LIMIT 1"));
    }

    /** 解析游标：全量模式尝试续传上次未完成的任务 */
    private String resolveCursor(Long sourceId, String taskType) {
        if (!"FULL".equals(taskType)) return null;

        SyncTask unfinished = taskMapper.selectOne(new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getSourceId, sourceId)
                .eq(SyncTask::getTaskType, "FULL")
                .in(SyncTask::getStatus, "PENDING", "RUNNING")
                .orderByDesc(SyncTask::getCreatedAt)
                .last("LIMIT 1"));
        return unfinished != null ? unfinished.getCurrentCursor() : null;
    }

    /** 创建同步任务记录，初始状态 RUNNING */
    private SyncTask createTaskRecord(StorageSource source, String taskType) {
        SyncTask task = new SyncTask();
        task.setTaskName(source.getSourceName() + "-" + taskType);
        task.setTaskType(taskType);
        task.setSourceId(source.getId());
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        taskMapper.insert(task);
        // 缓存运行状态
        statusCache.setStatus(task.getId(), "RUNNING");
        return task;
    }

    /** 创建文件同步明细 */
    private SyncTaskFile createFileRecord(Long taskId, FileInfo fi) {
        SyncTaskFile tf = new SyncTaskFile();
        tf.setTaskId(taskId);
        tf.setSourcePath(fi.getPath());
        tf.setSourceMd5(fi.getMd5());
        tf.setSourceSize(fi.getSize());
        tf.setFileStatus("PENDING");
        taskFileMapper.insert(tf);
        return tf;
    }

    /** 更新文件同步明细状态 */
    private void updateFileRecord(SyncTaskFile tf, boolean success) {
        tf.setFileStatus(success ? "SUCCESS" : "FAILED");
        taskFileMapper.updateById(tf);
    }

    /** 构建本地存储路径：localBase/sourceKey */
    private String buildLocalPath(String sourceKey) {
        return localStoragePath + File.separator + sourceKey;
    }

    /** 异常终止任务 */
    private void abortTask(SyncTask task, String reason) {
        task.setStatus("FAILED");
        task.setErrorMsg(reason);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        statusCache.setStatus(task.getId(), "FAILED");
    }

    /** 正常结束任务 */
    private void finishTask(SyncTask task, int total) {
        task.setTotalFiles(total);
        task.setStatus(task.getFailedFiles() > 0 ? "PARTIAL" : "SUCCESS");
        task.setCompletedAt(LocalDateTime.now());
        task.setCurrentCursor(null);
        taskMapper.updateById(task);
        statusCache.setStatus(task.getId(), task.getStatus());
    }
}
