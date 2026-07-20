package com.example.syncmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务：防止同步任务重复执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /** 锁前缀 */
    private static final String LOCK_PREFIX = "sync:lock:";

    /**
     * 尝试获取分布式锁（非阻塞）
     * @param sourceId  存储源 ID
     * @param waitSeconds 最大等待时间（秒）
     * @param leaseSeconds 锁持有时间（秒）
     * @return true-获取成功, false-获取失败（任务正在执行）
     */
    public boolean tryLock(Long sourceId, long waitSeconds, long leaseSeconds) {
        String key = LOCK_PREFIX + sourceId;
        RLock lock = redissonClient.getLock(key);
        try {
            boolean acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.info("获取分布式锁成功: sourceId={}", sourceId);
            } else {
                log.warn("获取分布式锁失败（任务正在执行）: sourceId={}", sourceId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: sourceId={}", sourceId, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    public void unlock(Long sourceId) {
        String key = LOCK_PREFIX + sourceId;
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放分布式锁: sourceId={}", sourceId);
        }
    }
}
