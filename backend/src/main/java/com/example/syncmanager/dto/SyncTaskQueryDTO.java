package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class SyncTaskQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private String taskType;
    private String status;
    private Long sourceId;
}
