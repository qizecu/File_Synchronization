package com.example.syncmanager.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.syncmanager.dto.SyncTaskVO;
import com.example.syncmanager.entity.SyncTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncTaskMapper extends BaseMapper<SyncTask> {

    @Select("SELECT t.id, t.task_name AS taskName, s.source_name AS sourceName, "
            + "t.task_type AS taskType, t.status, t.total_files AS totalFiles, "
            + "t.success_files AS successFiles, t.failed_files AS failedFiles, "
            + "t.started_at AS startedAt, t.completed_at AS completedAt "
            + "FROM sync_task t "
            + "LEFT JOIN storage_source s ON t.source_id = s.id AND s.is_deleted = 0 "
            + "${ew.customSqlSegment} "
            + "ORDER BY t.created_at DESC")
    IPage<SyncTaskVO> selectTaskVOPage(IPage<SyncTaskVO> page, @Param(Constants.WRAPPER) Wrapper<SyncTask> wrapper);
}
