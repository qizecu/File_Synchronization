package com.example.syncmanager.service.adapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储端文件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {

    /** 文件路径（对象 Key） */
    private String path;

    /** 文件大小（字节） */
    private Long size;

    /** 文件 ETag / MD5 */
    private String md5;

    /** 最后修改时间（用于增量同步判断） */
    private LocalDateTime lastModified;
}
