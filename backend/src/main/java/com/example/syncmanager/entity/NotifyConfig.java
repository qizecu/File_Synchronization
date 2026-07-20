package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_config")
public class NotifyConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置名称 */
    private String configName;

    /** 通知类型: DINGTALK / WECOM */
    private String notifyType;

    /** Webhook 地址 */
    private String webhookUrl;

    /** 签名密钥（可选） */
    private String secret;

    /** 是否启用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
