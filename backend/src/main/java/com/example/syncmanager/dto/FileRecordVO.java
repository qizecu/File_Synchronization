package com.example.syncmanager.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileRecordVO {

    private Long id;
    private String fileName;
    private String fileOrigin;
    private Long userId;
    private String userNickname;
    private String sourceName;
    private Long fileSize;
    private String fileStatus;
    private String errorMsg;
    private LocalDateTime createdAt;
}
