package com.example.syncmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dashboard 统计数据
 */
@Data
@Builder
public class DashboardStatsVO {

    /** 今日同步任务数 */
    private Long todaySyncCount;

    /** 累计同步文件数 */
    private Long totalSyncedFiles;

    /** 总存储大小（字节） */
    private Long totalStorageSize;

    /** 总存储源数 */
    private Long totalSources;

    /** 成功率（百分比，如 95.5） */
    private Double successRate;

    /** 磁盘使用率（百分比，如 45.2） */
    private Double diskUsage;

    /** 磁盘总大小（字节） */
    private Long diskTotal;

    /** 磁盘可用空间（字节） */
    private Long diskUsable;

    /** 最近同步任务列表 */
    private List<RecentTaskVO> recentTasks;

    @Data
    @Builder
    public static class RecentTaskVO {
        private Long id;
        private String taskName;
        private String taskType;
        private String status;
        private Integer totalFiles;
        private Integer successFiles;
        private String createdAt;
    }
}
