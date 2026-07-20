package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_task")
public class SyncTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String taskName;

    /** 任务类型: FULL(全量) / INCREMENTAL(增量) */
    private String taskType;

    /** 关联存储源ID */
    private Long sourceId;

    /** 任务状态: PENDING / RUNNING / SUCCESS / FAILED / PARTIAL */
    private String status;

    /** 总文件数 */
    private Integer totalFiles;

    /** 成功文件数 */
    private Integer successFiles;

    /** 失败文件数 */
    private Integer failedFiles;

    /** 跳过文件数 */
    private Integer skippedFiles;

    /** 断点续传游标 */
    private String currentCursor;

    /** 错误信息 */
    private String errorMsg;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
