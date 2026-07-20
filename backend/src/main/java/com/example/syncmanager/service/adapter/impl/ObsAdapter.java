package com.example.syncmanager.service.adapter.impl;

import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.service.adapter.FileInfo;
import com.example.syncmanager.service.adapter.StorageAdapter;
import com.obs.services.ObsClient;
import com.obs.services.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 华为 OBS 存储适配器
 */
@Slf4j
public class ObsAdapter implements StorageAdapter {

    private final ObsClient client;

    public ObsAdapter(String endpoint, String accessKey, String secretKey) {
        this.client = new ObsClient(accessKey, secretKey, endpoint);
    }

    @Override
    public boolean testConnection() {
        try {
            client.listBuckets();
            return true;
        } catch (Exception e) {
            log.error("OBS 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<FileInfo> listFiles(String bucket, String prefix, String cursor, int limit) {
        List<FileInfo> result = new ArrayList<>();
        boolean foundCursor = (cursor == null || cursor.isEmpty());

        ListObjectsRequest request = new ListObjectsRequest(bucket);
        request.setPrefix(prefix);
        request.setMaxKeys(limit);
        if (cursor != null && !cursor.isEmpty()) {
            request.setMarker(cursor);
        }

        ObjectListing listing = client.listObjects(request);
        for (ObsObject obj : listing.getObjects()) {
            if (obj.getObjectKey().endsWith("/")) continue; // 跳过目录

            String path = obj.getObjectKey();
            if (!foundCursor) {
                if (path.equals(cursor)) {
                    foundCursor = true;
                }
                continue;
            }

            // OBS 的 ETag 去引号
            String etag = obj.getMetadata() != null ? obj.getMetadata().getEtag() : null;
            if (etag != null) {
                etag = etag.replace("\"", "");
            }

            result.add(new FileInfo(
                    path,
                    obj.getMetadata() != null ? obj.getMetadata().getContentLength() : 0L,
                    etag,
                    obj.getMetadata() != null
                            ? obj.getMetadata().getLastModified().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null
            ));
        }

        return result;
    }

    @Override
    public FileInfo getFileInfo(String bucket, String path) {
        try {
            ObjectMetadata metadata = client.getObjectMetadata(bucket, path);
            String etag = metadata.getEtag();
            if (etag != null) {
                etag = etag.replace("\"", "");
            }
            return new FileInfo(
                    path,
                    metadata.getContentLength(),
                    etag,
                    metadata.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
        } catch (Exception e) {
            log.debug("获取文件信息失败: bucket={}, path={}, error={}", bucket, path, e.getMessage());
            return null;
        }
    }

    @Override
    public void downloadFile(String bucket, String path, String localPath) {
        try {
            // 确保父目录存在
            File localFile = new File(localPath);
            File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            DownloadFileRequest request = new DownloadFileRequest(bucket, path);
            request.setDownloadFile(localPath);
            client.downloadFile(request);
            log.debug("下载成功: bucket={}, path={}, local={}", bucket, path, localPath);
        } catch (Exception e) {
            throw new BusinessException("OBS 下载失败: " + e.getMessage());
        }
    }
}
