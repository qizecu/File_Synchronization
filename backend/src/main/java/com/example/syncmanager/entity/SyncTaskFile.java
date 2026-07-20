package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_task_file")
public class SyncTaskFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联同步任务ID */
    private Long taskId;

    /** 源端文件路径 */
    private String sourcePath;

    /** 源端文件 MD5 */
    private String sourceMd5;

    /** 源端文件大小（字节） */
    private Long sourceSize;

    /** 本地目标路径 */
    private String targetPath;

    /** 本地文件 MD5 */
    private String targetMd5;

    /** 本地文件大小（字节） */
    private Long targetSize;

    /** 文件状态: PENDING / SUCCESS / FAILED / SKIPPED */
    private String fileStatus;

    /** 已重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
