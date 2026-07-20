package com.example.syncmanager.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户昵称 */
    private String nickname;

    /** 用户名 */
    private String username;

    /** 用户角色：ADMIN / USER */
    private String role;
}
