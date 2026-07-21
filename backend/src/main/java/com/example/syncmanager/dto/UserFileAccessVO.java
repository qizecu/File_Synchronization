package com.example.syncmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件访问权限响应
 */
@Data
@Builder
public class UserFileAccessVO {
    private Long id;
    private Long userId;
    private String username;
    private String filePath;
    private Long grantedBy;
    private LocalDateTime createdAt;
}
