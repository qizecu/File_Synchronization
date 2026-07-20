package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.DashboardStatsVO;
import com.example.syncmanager.entity.SyncTask;
import com.example.syncmanager.entity.SyncTaskFile;
import com.example.syncmanager.mapper.StorageSourceMapper;
import com.example.syncmanager.mapper.SyncTaskFileMapper;
import com.example.syncmanager.mapper.SyncTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dashboard 统计数据
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SyncTaskMapper taskMapper;
    private final SyncTaskFileMapper taskFileMapper;
    private final StorageSourceMapper sourceMapper;

    @Value("${sync.local.storage-path}")
    private String localStoragePath;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        // 今日同步任务数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todaySyncCount = taskMapper.selectCount(
                new LambdaQueryWrapper<SyncTask>()
                        .ge(SyncTask::getCreatedAt, todayStart));

        // 累计成功同步文件数
        Long totalSyncedFiles = taskFileMapper.selectCount(
                new LambdaQueryWrapper<SyncTaskFile>()
                        .eq(SyncTaskFile::getFileStatus, "SUCCESS"));

        // 总存储大小（已成功同步文件的目标大小之和）
        Long totalStorageSize = 0L;
        try {
            List<SyncTaskFile> successFiles = taskFileMapper.selectList(
                    new LambdaQueryWrapper<SyncTaskFile>()
                            .select(SyncTaskFile::getTargetSize)
                            .eq(SyncTaskFile::getFileStatus, "SUCCESS"));
            totalStorageSize = successFiles.stream()
                    .mapToLong(f -> f.getTargetSize() != null ? f.getTargetSize() : 0L)
                    .sum();
        } catch (Exception ignored) {
            // 聚合查询失败则返回 0
        }

        // 总存储源数
        Long totalSources = sourceMapper.selectCount(null);

        // 成功率（已完成任务中成功的比例）
        Double successRate = 100.0;
        try {
            Long finishedCount = taskMapper.selectCount(
                    new LambdaQueryWrapper<SyncTask>()
                            .in(SyncTask::getStatus, "SUCCESS", "FAILED", "PARTIAL"));
            Long successCount = taskMapper.selectCount(
                    new LambdaQueryWrapper<SyncTask>()
                            .eq(SyncTask::getStatus, "SUCCESS"));
            if (finishedCount > 0) {
                successRate = successCount * 100.0 / finishedCount;
                successRate = Math.round(successRate * 10.0) / 10.0; // 保留 1 位小数
            }
        } catch (Exception ignored) {
            // ignore
        }

        // 磁盘使用率
        File dir = new File(localStoragePath);
        if (!dir.exists()) dir.mkdirs();
        long diskTotal = dir.getTotalSpace();
        long diskUsable = dir.getUsableSpace();
        double diskUsage = Math.round((1.0 - (double) diskUsable / diskTotal) * 1000.0) / 10.0;

        // 最近任务（最多 5 条）
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<DashboardStatsVO.RecentTaskVO> recentTasks = taskMapper.selectList(
                new LambdaQueryWrapper<SyncTask>()
                        .orderByDesc(SyncTask::getCreatedAt)
                        .last("LIMIT 5"))
                .stream()
                .map(t -> DashboardStatsVO.RecentTaskVO.builder()
                        .id(t.getId())
                        .taskName(t.getTaskName())
                        .taskType(t.getTaskType())
                        .status(t.getStatus())
                        .totalFiles(t.getTotalFiles())
                        .successFiles(t.getSuccessFiles())
                        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : null)
                        .build())
                .toList();

        DashboardStatsVO vo = DashboardStatsVO.builder()
                .todaySyncCount(todaySyncCount)
                .totalSyncedFiles(totalSyncedFiles)
                .totalStorageSize(totalStorageSize)
                .totalSources(totalSources)
                .successRate(successRate)
                .diskUsage(diskUsage)
                .diskTotal(diskTotal)
                .diskUsable(diskUsable)
                .recentTasks(recentTasks)
                .build();

        return Result.success(vo);
    }
}
