<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import type { DashboardStatsVO } from '@/types/api'

const loading = ref(false)
const stats = ref<DashboardStatsVO>({
  todaySyncCount: 0,
  totalSyncedFiles: 0,
  totalStorageSize: 0,
  totalSources: 0,
  successRate: 100,
  diskUsage: 0,
  diskTotal: 0,
  diskUsable: 0,
  recentTasks: [],
})

async function loadData() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

/** 磁盘使用率进度条颜色 */
function diskColor(percent: number): string {
  if (percent >= 95) return '#F56C6C'
  if (percent >= 90) return '#E6A23C'
  if (percent >= 80) return '#409EFF'
  return '#67C23A'
}

/** 磁盘状态文本 */
function diskStatusText(percent: number): string {
  if (percent >= 95) return '紧急：磁盘即将写满！'
  if (percent >= 90) return '严重：磁盘空间严重不足'
  if (percent >= 80) return '预警：磁盘使用率较高'
  return '正常'
}

function diskStatusType(percent: number): 'danger' | 'warning' | 'success' {
  if (percent >= 95) return 'danger'
  if (percent >= 90) return 'danger'
  if (percent >= 80) return 'warning'
  return 'success'
}

/** 任务状态标签 */
function statusType(status: string): 'success' | 'danger' | 'warning' | 'info' {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    SUCCESS: 'success',
    FAILED: 'danger',
    PARTIAL: 'warning',
    RUNNING: '' as 'info',
    PENDING: 'info',
  }
  return map[status] || 'info'
}

function statusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '执行中',
    SUCCESS: '成功',
    FAILED: '失败',
    PARTIAL: '部分成功',
  }
  return map[status] || status
}

function formatSize(bytes: number): string {
  if (!bytes || bytes === 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background: #ecf5ff">
              <el-icon size="28" color="#409EFF"><Odometer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.todaySyncCount }}</div>
              <div class="stat-label">今日同步任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background: #f0f9eb">
              <el-icon size="28" color="#67C23A"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalSyncedFiles.toLocaleString() }}</div>
              <div class="stat-label">累计同步文件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background: #fdf6ec">
              <el-icon size="28" color="#E6A23C"><Folder /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ formatSize(stats.totalStorageSize) }}</div>
              <div class="stat-label">总存储大小</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background: #fef0f0">
              <el-icon size="28" color="#F56C6C"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.successRate }}%</div>
              <div class="stat-label">成功率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 磁盘使用率 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <span class="card-header-title">磁盘使用率</span>
      </template>
      <div class="disk-section">
        <el-progress
          :percentage="stats.diskUsage"
          :color="diskColor(stats.diskUsage)"
          :stroke-width="20"
          :text-inside="true"
        />
        <div class="disk-info">
          <div class="disk-tips">
            <el-tag :type="diskStatusType(stats.diskUsage)" size="small">
              {{ diskStatusText(stats.diskUsage) }}
            </el-tag>
            <span class="disk-thresholds">三级预警阈值：80% / 90% / 95%</span>
          </div>
          <div class="disk-detail">
            总空间：{{ formatSize(stats.diskTotal) }} &nbsp;|&nbsp;
            可用：{{ formatSize(stats.diskUsable) }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 最近任务 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <span class="card-header-title">最近同步任务</span>
      </template>
      <el-table :data="stats.recentTasks" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" />
        <el-table-column prop="taskType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 'FULL' ? 'primary' : 'success'" size="small">
              {{ row.taskType === 'FULL' ? '全量' : '增量' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="100">
          <template #default="{ row }">
            <template v-if="row.totalFiles > 0">
              {{ row.successFiles }} / {{ row.totalFiles }}
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
      <div v-if="stats.recentTasks.length === 0" class="empty-hint">暂无同步任务</div>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: default;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.section-card {
  margin-bottom: 20px;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
}

/* 磁盘 */
.disk-section {
  padding: 10px 0;
}

.disk-info {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.disk-tips {
  display: flex;
  align-items: center;
  gap: 12px;
}

.disk-thresholds {
  color: #999;
  font-size: 12px;
}

.disk-detail {
  color: #999;
  font-size: 13px;
}

.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}
</style>
