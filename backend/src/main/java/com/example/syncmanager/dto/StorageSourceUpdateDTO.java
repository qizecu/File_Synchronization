package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class StorageSourceUpdateDTO {

    private String sourceName;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;
    private String prefixPath;
    private Boolean enabled;
}
