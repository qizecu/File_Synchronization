package com.example.syncmanager.scheduler;

import com.example.syncmanager.entity.StorageSource;
import com.example.syncmanager.mapper.StorageSourceMapper;
import com.example.syncmanager.service.SyncOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时调度 -- 每天凌晨增量同步所有启用的存储源
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final StorageSourceMapper sourceMapper;
    private final SyncOrchestrator orchestrator;

    /** 每天凌晨 2 点执行增量同步 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyIncrementalSync() {
        log.info("===== 定时增量同步开始 =====");
        List<StorageSource> sources = sourceMapper.selectList(null);

        for (StorageSource source : sources) {
            if (!source.getEnabled()) {
                log.info("存储源已禁用，跳过: {}", source.getSourceName());
                continue;
            }
            try {
                orchestrator.executeIncrementalSync(source.getId());
            } catch (Exception e) {
                log.error("增量同步异常: source={}, error={}", source.getSourceName(), e.getMessage());
            }
        }
        log.info("===== 定时增量同步结束 =====");
    }
}
