package com.example.syncmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件授权/撤销请求参数
 */
@Data
public class GrantAccessDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "文件路径不能为空")
    private String filePath;
}
