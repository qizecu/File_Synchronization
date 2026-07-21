package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("storage_source")
public class StorageSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储源名称 */
    private String sourceName;

    /** 存储类型: MINIO / OBS */
    private String sourceType;

    /** 端点地址 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 私有密钥 */
    private String secretKey;

    /** 桶名称 */
    private String bucket;

    /** 区域（OBS 需要） */
    private String region;

    /** 文件前缀路径 */
    private String prefixPath;

    /** 是否启用 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
