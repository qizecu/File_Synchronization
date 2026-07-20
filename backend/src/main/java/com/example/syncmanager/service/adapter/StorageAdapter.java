package com.example.syncmanager.service.adapter;

import java.util.List;

/**
 * 存储适配器接口 -- 统一封装 MinIO / OBS 的文件操作
 */
public interface StorageAdapter {

    /**
     * 测试存储源连通性
     */
    boolean testConnection();

    /**
     * 列出文件（分页 + 游标支持断点续传）
     *
     * @param bucket  桶名称
     * @param prefix  文件前缀路径
     * @param cursor  游标标记（上次列举到的最后一个文件路径，null 表示从头开始）
     * @param limit   每次返回数量上限
     * @return 文件信息列表
     */
    List<FileInfo> listFiles(String bucket, String prefix, String cursor, int limit);

    /**
     * 获取单个文件元信息
     *
     * @param bucket 桶名称
     * @param path   对象路径
     * @return 文件信息（路径不存在时返回 null）
     */
    FileInfo getFileInfo(String bucket, String path);

    /**
     * 下载文件到本地路径
     *
     * @param bucket    桶名称
     * @param path      对象路径
     * @param localPath 本地目标路径
     */
    void downloadFile(String bucket, String path, String localPath);
}
