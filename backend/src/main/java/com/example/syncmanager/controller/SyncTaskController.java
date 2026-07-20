package com.example.syncmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.dto.SyncTaskFileQueryDTO;
import com.example.syncmanager.dto.SyncTaskQueryDTO;
import com.example.syncmanager.entity.SyncTask;
import com.example.syncmanager.entity.SyncTaskFile;
import com.example.syncmanager.mapper.SyncTaskFileMapper;
import com.example.syncmanager.mapper.SyncTaskMapper;
import com.example.syncmanager.service.SyncOrchestrator;
import com.example.syncmanager.service.SyncStatusCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync-tasks")
@RequiredArgsConstructor
public class SyncTaskController {

    private final SyncTaskMapper taskMapper;
    private final SyncTaskFileMapper taskFileMapper;
    private final SyncOrchestrator orchestrator;
    private final SyncStatusCacheService statusCacheService;

    /** 任务列表（分页） */
    @GetMapping
    public Result<Page<SyncTask>> list(SyncTaskQueryDTO query) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<SyncTask>()
                .eq(query.getTaskType() != null, SyncTask::getTaskType, query.getTaskType())
                .eq(query.getStatus() != null, SyncTask::getStatus, query.getStatus())
                .eq(query.getSourceId() != null, SyncTask::getSourceId, query.getSourceId())
                .orderByDesc(SyncTask::getCreatedAt);
        Page<SyncTask> page = taskMapper.selectPage(
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

    /** 触发全量同步 */
    @PostMapping("/trigger/full/{sourceId}")
    public Result<Void> triggerFull(@PathVariable Long sourceId) {
        orchestrator.executeFullSync(sourceId);
        return Result.success();
    }

    /** 触发增量同步 */
    @PostMapping("/trigger/incremental/{sourceId}")
    public Result<Void> triggerIncremental(@PathVariable Long sourceId) {
        orchestrator.executeIncrementalSync(sourceId);
        return Result.success();
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
}
