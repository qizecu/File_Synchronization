package com.example.syncmanager.service.adapter;

import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.entity.StorageSource;
import com.example.syncmanager.service.adapter.impl.MinioAdapter;
import com.example.syncmanager.service.adapter.impl.ObsAdapter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储适配器工厂 -- 按存储源 ID 缓存适配器实例，避免重复创建客户端连接
 */
public class StorageAdapterFactory {

    private static final ConcurrentHashMap<Long, StorageAdapter> CACHE = new ConcurrentHashMap<>();

    /**
     * 获取或创建适配器实例
     */
    public static StorageAdapter getOrCreate(StorageSource source) {
        return CACHE.computeIfAbsent(source.getId(), id -> buildAdapter(source));
    }

    /**
     * 移除缓存（配置变更时调用）
     */
    public static void invalidate(Long sourceId) {
        CACHE.remove(sourceId);
    }

    private static StorageAdapter buildAdapter(StorageSource source) {
        String type = source.getSourceType().toUpperCase();
        return switch (type) {
            case "MINIO" -> new MinioAdapter(
                    source.getEndpoint(),
                    source.getAccessKey(),
                    source.getSecretKey()
            );
            case "OBS" -> new ObsAdapter(
                    source.getEndpoint(),
                    source.getAccessKey(),
                    source.getSecretKey()
            );
            default -> throw new BusinessException("不支持的存储类型: " + type);
        };
    }
}
