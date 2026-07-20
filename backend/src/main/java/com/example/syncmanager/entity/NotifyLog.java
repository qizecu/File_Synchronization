package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_log")
public class NotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联同步任务ID */
    private Long taskId;

    /** 关联通知配置ID */
    private Long notifyConfigId;

    /** 通知类型 */
    private String notifyType;

    /** 通知状态: PENDING / SUCCESS / FAILED */
    private String notifyStatus;

    /** 请求内容 */
    private String requestBody;

    /** 响应内容 */
    private String responseBody;

    /** 错误信息 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer isDeleted;
}
