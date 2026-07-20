<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listSyncTasks,
  getSyncTaskDetail,
  listTaskFiles,
  triggerFullSync,
  triggerIncrementalSync,
} from '@/api/syncTask'
import { listStorageSources } from '@/api/storageSource'
import type { SyncTask, SyncTaskFile, SyncTaskQuery, StorageSource } from '@/types/api'

const loading = ref(false)
const tableData = ref<SyncTask[]>([])
const total = ref(0)
const sources = ref<StorageSource[]>([])

// 查询参数
const query = reactive<SyncTaskQuery>({
  page: 1,
  size: 10,
  status: '',
  taskType: '',
  sourceId: undefined,
})

// 详情抽屉
const drawerVisible = ref(false)
const currentTask = ref<SyncTask | null>(null)
const fileList = ref<SyncTaskFile[]>([])
const fileTotal = ref(0)
const fileLoading = ref(false)
const fileQuery = reactive({ page: 1, size: 10, fileStatus: '' })

// 触发同步
const triggerLoading = ref<{ full: boolean; incremental: boolean }>({ full: false, incremental: false })
const triggerDialogVisible = ref(false)
const selectedSourceId = ref<number>()

/** 获取存储源来源名称 */
function sourceName(id: number): string {
  return sources.value.find((s) => s.id === id)?.sourceName || `ID: ${id}`
}

async function loadSources() {
  try {
    sources.value = await listStorageSources()
  } catch {
    // ignore
  }
}

/** 查询任务列表 */
async function loadData() {
  loading.value = true
  try {
    const res = await listSyncTasks(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 分页 */
function handlePageChange(page: number) {
  query.page = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  loadData()
}

/** 状态标签 */
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

/** 查看任务详情 */
async function handleViewDetail(row: SyncTask) {
  drawerVisible.value = true
  fileLoading.value = true
  try {
    currentTask.value = await getSyncTaskDetail(row.id)
  } catch {
    currentTask.value = row
  }
  try {
    const res = await listTaskFiles(row.id, { page: 1, size: 10 })
    fileList.value = res.records
    fileTotal.value = res.total
    fileQuery.page = 1
  } finally {
    fileLoading.value = false
  }
}

/** 详情中切换文件分页 */
async function handleFilePageChange(page: number) {
  fileQuery.page = page
  fileLoading.value = true
  try {
    const res = await listTaskFiles(currentTask.value!.id, {
      page: fileQuery.page,
      size: fileQuery.size,
      fileStatus: fileQuery.fileStatus || undefined,
    })
    fileList.value = res.records
    fileTotal.value = res.total
  } finally {
    fileLoading.value = false
  }
}

/** 触发同步弹窗 */
function openTriggerDialog() {
  selectedSourceId.value = sources.value.find((s) => s.enabled)?.id
  triggerDialogVisible.value = true
}

async function doTrigger(type: 'FULL' | 'INCREMENTAL') {
  if (!selectedSourceId.value) {
    ElMessage.warning('请选择存储源')
    return
  }
  const key: 'full' | 'incremental' = type === 'FULL' ? 'full' : 'incremental'
  triggerLoading.value[key] = true
  try {
    if (type === 'FULL') {
      await triggerFullSync(selectedSourceId.value)
    } else {
      await triggerIncrementalSync(selectedSourceId.value)
    }
    ElMessage.success(`${type === 'FULL' ? '全量' : '增量'}同步已触发，请稍后刷新查看结果`)
    triggerDialogVisible.value = false
    loadData()
  } finally {
    triggerLoading.value[key] = false
  }
}

function fileStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    SUCCESS: '成功',
    FAILED: '失败',
    SKIPPED: '已跳过',
  }
  return map[status] || status
}

function fileStatusType(status: string): 'success' | 'danger' | 'info' | 'warning' {
  const map: Record<string, 'success' | 'danger' | 'info' | 'warning'> = {
    PENDING: 'info',
    SUCCESS: 'success',
    FAILED: 'danger',
    SKIPPED: 'warning',
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadSources()
  loadData()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="任务类型">
          <el-select v-model="query.taskType" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="全量" value="FULL" />
            <el-option label="增量" value="INCREMENTAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="等待中" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="部分成功" value="PARTIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储源">
          <el-select
            v-model="query.sourceId"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="loadData"
          >
            <el-option
              v-for="s in sources"
              :key="s.id"
              :label="s.sourceName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="openTriggerDialog">
            <el-icon><VideoPlay /></el-icon>
            触发同步
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="存储源" width="140">
          <template #default="{ row }">{{ sourceName(row.sourceId) }}</template>
        </el-table-column>
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
        <el-table-column label="进度" min-width="120">
          <template #default="{ row }">
            <template v-if="row.totalFiles > 0">
              <el-progress
                :percentage="Math.round((row.successFiles / row.totalFiles) * 100)"
                :stroke-width="8"
                :show-text="false"
              />
              <span class="progress-text">{{ row.successFiles }}/{{ row.totalFiles }}</span>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170" />
        <el-table-column prop="completedAt" label="完成时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row as SyncTask)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size!"
          :current-page="query.page!"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 任务详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="任务详情"
      size="650px"
    >
      <template v-if="currentTask">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
          <el-descriptions-item label="任务类型">
            <el-tag :type="currentTask.taskType === 'FULL' ? 'primary' : 'success'" size="small">
              {{ currentTask.taskType === 'FULL' ? '全量' : '增量' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentTask.status)" size="small">
              {{ statusText(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="存储源 ID">{{ currentTask.sourceId }}</el-descriptions-item>
          <el-descriptions-item label="总文件">{{ currentTask.totalFiles }}</el-descriptions-item>
          <el-descriptions-item label="成功">{{ currentTask.successFiles }}</el-descriptions-item>
          <el-descriptions-item label="失败">{{ currentTask.failedFiles }}</el-descriptions-item>
          <el-descriptions-item label="跳过">{{ currentTask.skippedFiles }}</el-descriptions-item>
          <el-descriptions-item label="当前游标" :span="2">{{ currentTask.currentCursor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2">
            <span class="error-text">{{ currentTask.errorMsg || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ currentTask.startedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ currentTask.completedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 文件明细 -->
        <h4 style="margin: 16px 0 8px">文件明细</h4>
        <el-table :data="fileList" v-loading="fileLoading" size="small" style="width: 100%">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="sourcePath" label="文件路径" min-width="200" show-overflow-tooltip />
          <el-table-column prop="fileStatus" label="状态" width="80">
            <template #default="{ row: f }">
              <el-tag :type="fileStatusType((f as SyncTaskFile).fileStatus)" size="small">
                {{ fileStatusText((f as SyncTaskFile).fileStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="retryCount" label="重试" width="50" />
          <el-table-column prop="errorMsg" label="错误信息" min-width="100" show-overflow-tooltip>
            <template #default="{ row: f }">{{ f.errorMsg || '-' }}</template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap" v-if="fileTotal > 0">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="fileTotal"
            :page-size="fileQuery.size"
            :current-page="fileQuery.page"
            small
            @current-change="handleFilePageChange"
          />
        </div>
      </template>
    </el-drawer>

    <!-- 触发同步弹窗 -->
    <el-dialog v-model="triggerDialogVisible" title="触发同步" width="450px">
      <el-form label-width="100px">
        <el-form-item label="选择存储源">
          <el-select v-model="selectedSourceId" style="width: 100%" placeholder="请选择">
            <el-option
              v-for="s in sources.filter((s) => s.enabled)"
              :key="s.id"
              :label="s.sourceName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="triggerLoading.full"
          @click="doTrigger('FULL')"
        >
          全量同步
        </el-button>
        <el-button
          type="success"
          :loading="triggerLoading.incremental"
          @click="doTrigger('INCREMENTAL')"
        >
          增量同步
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 0;
}

.progress-text {
  margin-left: 8px;
  font-size: 12px;
  color: #666;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.error-text {
  color: #f56c6c;
}
</style>
