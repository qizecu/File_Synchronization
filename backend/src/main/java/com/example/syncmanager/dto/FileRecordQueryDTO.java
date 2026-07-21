package com.example.syncmanager.dto;

import lombok.Data;

@Data
public class FileRecordQueryDTO {

    private Integer page = 1;
    private Integer size = 10;

    /** 来源筛选: SYNC / UPLOAD / ALL */
    private String type;

    /** 用户ID（仅 ADMIN 可用） */
    private Long userId;

    /** 状态筛选: PENDING / SUCCESS / FAILED */
    private String status;

    /** 文件名模糊搜索 */
    private String fileName;
}
