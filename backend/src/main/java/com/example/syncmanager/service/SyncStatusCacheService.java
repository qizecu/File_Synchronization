package com.example.syncmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 同步任务状态缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStatusCacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "sync:task:status:";
    private static final Duration TTL = Duration.ofHours(1);

    /** 缓存任务状态 */
    public void setStatus(Long taskId, String status) {
        String key = CACHE_PREFIX + taskId;
        redisTemplate.opsForValue().set(key, status, TTL);
        log.debug("缓存同步状态: taskId={}, status={}", taskId, status);
    }

    /** 获取任务状态 */
    public String getStatus(Long taskId) {
        String key = CACHE_PREFIX + taskId;
        String value = redisTemplate.opsForValue().get(key);
        return value;
    }

    /** 清除缓存 */
    public void clearStatus(Long taskId) {
        String key = CACHE_PREFIX + taskId;
        redisTemplate.delete(key);
    }
}
