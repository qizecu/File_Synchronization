package com.example.syncmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
}
