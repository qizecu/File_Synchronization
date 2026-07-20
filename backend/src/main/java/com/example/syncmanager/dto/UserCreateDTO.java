package com.example.syncmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增用户请求
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|USER", message = "角色必须为 ADMIN 或 USER")
    private String role;
}
