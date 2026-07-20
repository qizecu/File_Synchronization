package com.example.syncmanager.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 编辑用户请求（所有字段可选，只更新传入的字段）
 */
@Data
public class UserUpdateDTO {

    private String nickname;

    @Pattern(regexp = "ADMIN|USER", message = "角色必须为 ADMIN 或 USER")
    private String role;

    /** 状态：1-启用 0-禁用 */
    private Integer status;
}
