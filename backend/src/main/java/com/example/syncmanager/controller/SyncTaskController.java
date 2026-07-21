package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.SyncTaskFileQueryDTO;
import com.example.syncmanager.dto.SyncTaskQueryDTO;
import com.example.syncmanager.dto.SyncTaskVO;
import com.example.syncmanager.entity.SyncTask;
import com.example.syncmanager.entity.SyncTaskFile;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SyncTaskFileMapper;
import com.example.syncmanager.mapper.SyncTaskMapper;
import com.example.syncmanager.mapper.SysUserMapper;
import com.example.syncmanager.service.SyncOrchestrator;
import com.example.syncmanager.service.SyncStatusCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sync-tasks")
@RequiredArgsConstructor
public class SyncTaskController {

    private final SyncTaskMapper taskMapper;
    private final SyncTaskFileMapper taskFileMapper;
    private final SysUserMapper sysUserMapper;
    private final SyncOrchestrator orchestrator;
    private final SyncStatusCacheService statusCacheService;

    /** 任务列表（分页） */
    @GetMapping
    public Result<IPage<SyncTaskVO>> list(SyncTaskQueryDTO query) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<SyncTask>()
                .eq(query.getTaskType() != null, SyncTask::getTaskType, query.getTaskType())
                .eq(query.getStatus() != null, SyncTask::getStatus, query.getStatus())
                .eq(query.getSourceId() != null, SyncTask::getSourceId, query.getSourceId());
        IPage<SyncTaskVO> page = taskMapper.selectTaskVOPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        return Result.success(page);
    }

    /** 任务详情 */
    @GetMapping("/{id}")
    public Result<SyncTask> detail(@PathVariable Long id) {
        return Result.success(taskMapper.selectById(id));
    }

    /** 任务文件明细（分页） */
    @GetMapping("/{id}/files")
    public Result<Page<SyncTaskFile>> files(@PathVariable Long id, SyncTaskFileQueryDTO query) {
        LambdaQueryWrapper<SyncTaskFile> wrapper = new LambdaQueryWrapper<SyncTaskFile>()
                .eq(SyncTaskFile::getTaskId, id)
                .eq(query.getFileStatus() != null, SyncTaskFile::getFileStatus, query.getFileStatus())
                .orderByDesc(SyncTaskFile::getCreatedAt);
        return Result.success(taskFileMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper));
    }

    /** 触发全量同步（异步执行，立即返回任务ID） */
    @PostMapping("/trigger/full/{sourceId}")
    public Result<Map<String, Object>> triggerFull(@PathVariable Long sourceId) {
        Long taskId = orchestrator.executeFullSync(sourceId, getCurrentUser().getId());
        log.info("全量同步已触发: sourceId={}, taskId={}", sourceId, taskId);
        return Result.success(Map.of("taskId", taskId));
    }

    /** 触发增量同步（异步执行，立即返回任务ID） */
    @PostMapping("/trigger/incremental/{sourceId}")
    public Result<Map<String, Object>> triggerIncremental(@PathVariable Long sourceId) {
        Long taskId = orchestrator.executeIncrementalSync(sourceId, getCurrentUser().getId());
        log.info("增量同步已触发: sourceId={}, taskId={}", sourceId, taskId);
        return Result.success(Map.of("taskId", taskId));
    }

    /** 重试失败文件 */
    @PostMapping("/files/{fileId}/retry")
    public Result<Boolean> retryFile(@PathVariable Long fileId) {
        boolean ok = orchestrator.retryFile(fileId);
        return Result.success(ok);
    }

    /** 快速查询任务状态（优先读 Redis 缓存） */
    @GetMapping("/{id}/status")
    public Result<String> getTaskStatus(@PathVariable Long id) {
        String cached = statusCacheService.getStatus(id);
        if (cached != null) {
            return Result.success(cached);
        }
        SyncTask task = taskMapper.selectById(id);
        return Result.success(task != null ? task.getStatus() : null);
    }

    /** 从 SecurityContext 获取当前用户 */
    private SysUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("无法识别当前用户");
        }
        return user;
    }
}
