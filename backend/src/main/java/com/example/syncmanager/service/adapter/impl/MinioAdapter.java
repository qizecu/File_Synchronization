package com.example.syncmanager.service.adapter.impl;

import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.service.adapter.FileInfo;
import com.example.syncmanager.service.adapter.StorageAdapter;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 存储适配器
 */
@Slf4j
public class MinioAdapter implements StorageAdapter {

    private final MinioClient client;

    public MinioAdapter(String endpoint, String accessKey, String secretKey) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region("us-east-1")  // 强制 path-style 访问（如果不设 region，SDK 默认 virtual-host-style）
                .build();
    }

    @Override
    public boolean testConnection() {
        try {
            client.listBuckets();
            return true;
        } catch (Exception e) {
            String msg = e.getMessage();
            // MinIO SDK 已知兼容性问题：部分服务端版本返回的 Bucket 列表缺少 Owner 元素，
            // 导致 XML 解析报错，但服务器已正常响应，连接实际上是通的
            if (msg != null && (msg.contains("Owner") || msg.contains("ValueRequiredException"))) {
                log.info("MinIO 连接成功（listBuckets XML 兼容性警告，不影响连接）");
                return true;
            }
            if (msg != null && msg.contains("API port")) {
                log.error("MinIO 连接测试失败 — 端点地址可能填了控制台端口，请改用 S3 API 端口");
            } else {
                log.error("MinIO 连接测试失败: {}", msg);
            }
            return false;
        }
    }

    @Override
    public List<FileInfo> listFiles(String bucket, String prefix, String cursor, int limit) {
        List<FileInfo> result = new ArrayList<>();
        boolean foundCursor = (cursor == null || cursor.isEmpty()); // cursor 为 null 表示从头开始

        Iterable<Result<Item>> items = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> resultItem : items) {
            if (result.size() >= limit) break;

            Item item = getItem(resultItem);
            if (item.isDir()) continue;

            String path = item.objectName();
            // 跳过已处理的游标位置
            if (!foundCursor) {
                if (path.equals(cursor)) {
                    foundCursor = true;
                }
                continue;
            }

            result.add(new FileInfo(
                    path,
                    item.size(),
                    item.etag(),
                    item.lastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            ));
        }

        return result;
    }

    @Override
    public FileInfo getFileInfo(String bucket, String path) {
        try {
            StatObjectResponse stat = client.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(path).build()
            );
            return new FileInfo(
                    path,
                    stat.size(),
                    stat.etag(),
                    stat.lastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
        } catch (Exception e) {
            log.debug("获取文件信息失败: bucket={}, path={}, error={}", bucket, path, e.getMessage());
            return null;
        }
    }

    @Override
    public void downloadFile(String bucket, String path, String localPath) {
        try {
            // 确保目标目录存在（包括所有父目录）
            File targetFile = new File(localPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            client.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .filename(localPath)
                            .build()
            );
            log.debug("下载成功: bucket={}, path={}, local={}", bucket, path, localPath);
        } catch (Exception e) {
            throw new BusinessException("MinIO 下载失败: " + e.getMessage());
        }
    }

    /** 安全解包 MinIO Result */
    private static Item getItem(Result<Item> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new BusinessException("MinIO 文件列表读取失败: " + e.getMessage());
        }
    }
}
