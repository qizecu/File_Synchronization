package com.example.syncmanager.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件/目录浏览条目
 */
@Data
@Builder
public class FileBrowseVO {

    /** 文件名 */
    private String name;

    /** 相对路径（用于后续下载/预览） */
    private String path;

    /** 文件大小（字节），目录为 0 */
    private Long size;

    /** 最后修改时间 */
    private String lastModified;

    /** 是否为目录 */
    private Boolean isDirectory;

    /** 文件类型（扩展名小写，如 jpg） */
    private String extension;

    /** 是否为图片类型（前端判断是否显示缩略图） */
    private Boolean isImage;
}
