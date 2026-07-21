package com.example.syncmanager.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncTaskVO {

    private Long id;
    private String taskName;
    private String sourceName;
    private String taskType;
    private String status;
    private Integer totalFiles;
    private Integer successFiles;
    private Integer failedFiles;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
