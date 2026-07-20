package com.example.syncmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StorageSourceCreateDTO {

    @NotBlank(message = "存储源名称不能为空")
    private String sourceName;

    @NotBlank(message = "存储类型不能为空")
    private String sourceType;

    @NotBlank(message = "端点地址不能为空")
    private String endpoint;

    @NotBlank(message = "访问密钥不能为空")
    private String accessKey;

    @NotBlank(message = "私有密钥不能为空")
    private String secretKey;

    @NotBlank(message = "桶名称不能为空")
    private String bucket;

    private String region;

    private String prefixPath;

    private Integer enabled;
}
