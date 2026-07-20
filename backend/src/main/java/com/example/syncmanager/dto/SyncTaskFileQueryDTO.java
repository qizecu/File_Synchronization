package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class SyncTaskFileQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private String fileStatus;
}
