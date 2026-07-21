package com.example.syncmanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件访问权限表：管理员给普通用户授权访问指定文件/目录
 */
@Data
@TableName("user_file_access")
public class UserFileAccess {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 授权的文件路径或目录路径 */
    private String filePath;

    /** 授权人（管理员ID） */
    private Long grantedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer isDeleted;
}
