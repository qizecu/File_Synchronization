package com.example.syncmanager.controller;

import com.example.syncmanager.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 分布式锁管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/redis/locks")
@RequiredArgsConstructor
public class RedisLockController {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "sync:lock:";

    /** 查看当前所有同步锁 */
    @GetMapping
    public Result<List<Map<String, Object>>> listLocks() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(LOCK_PREFIX + "*");
        List<Map<String, Object>> locks = new ArrayList<>();
        for (String key : keys) {
            Map<String, Object> info = new HashMap<>();
            info.put("key", key);
            info.put("sourceId", key.replace(LOCK_PREFIX, ""));
            // 获取锁的剩余 TTL（毫秒）
            long ttl = redissonClient.getKeys().remainTimeToLive(key);
            info.put("ttlMillis", ttl);
            locks.add(info);
        }
        return Result.success(locks);
    }

    /** 清除指定存储源的锁 */
    @DeleteMapping("/{sourceId}")
    public Result<String> deleteLock(@PathVariable Long sourceId) {
        String key = LOCK_PREFIX + sourceId;
        boolean deleted = redissonClient.getKeys().delete(key) > 0;
        if (deleted) {
            log.info("已清除锁: sourceId={}", sourceId);
            return Result.success("锁已清除: " + key);
        }
        return Result.success("锁不存在: " + key);
    }

    /** 清除所有同步锁 */
    @DeleteMapping
    public Result<Map<String, Object>> deleteAllLocks() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(LOCK_PREFIX + "*");
        int count = 0;
        for (String key : keys) {
            redissonClient.getKeys().delete(key);
            count++;
        }
        log.info("已清除所有同步锁，共 {} 个", count);
        return Result.success(Map.of("count", count, "message", "所有锁已清除"));
    }
}
