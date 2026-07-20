<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listSyncTasks, listTaskFiles, retryFile } from '@/api/syncTask'
import type { SyncTask, SyncTaskFile } from '@/types/api'

const loading = ref(false)
const tableData = ref<SyncTaskFile[]>([])
const total = ref(0)
const tasks = ref<SyncTask[]>([])

const query = reactive({
  page: 1,
  size: 15,
  fileStatus: '',
  taskId: undefined as number | undefined,
  fileName: '',
})

/** 加载任务列表（用于筛选） */
async function loadTasks() {
  try {
    const res = await listSyncTasks({ page: 1, size: 100 })
    tasks.value = res.records
  } catch {
    // ignore
  }
}

/** 间接查询文件：如果没有指定 taskId，遍历所有任务汇总；有 taskId 时直接查该任务的文件 */
async function loadData() {
  loading.value = true
  try {
    if (query.taskId) {
      // 指定任务
      const res = await listTaskFiles(query.taskId, {
        page: query.page,
        size: query.size,
        fileStatus: query.fileStatus || undefined,
      })
      tableData.value = res.records
      total.value = res.total
    } else {
      // 汇总所有任务的文件
      const allFiles: SyncTaskFile[] = []
      const tasksToQuery = tasks.value.slice(0, 10) // 限制查询最近10个任务
      for (const task of tasksToQuery) {
        try {
          const res = await listTaskFiles(task.id, {
            page: 1,
            size: 200,
            fileStatus: query.fileStatus || undefined,
          })
          allFiles.push(...res.records)
        } catch {
          // skip
        }
      }
      // 客户端筛选
      let filtered = allFiles
      if (query.fileName) {
        const keyword = query.fileName.toLowerCase()
        filtered = filtered.filter((f) => f.sourcePath.toLowerCase().includes(keyword))
      }
      if (query.fileStatus) {
        filtered = filtered.filter((f) => f.fileStatus === query.fileStatus)
      }
      total.value = filtered.length
      const start = (query.page - 1) * query.size
      tableData.value = filtered.slice(start, start + query.size)
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  loadData()
}

/** 文件状态映射 */
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

/** 重试失败文件 */
async function handleRetry(row: SyncTaskFile) {
  try {
    const ok = await retryFile(row.id)
    if (ok) {
      ElMessage.success('重试成功')
    } else {
      ElMessage.error('重试失败')
    }
    loadData()
  } catch {
    // 错误已由拦截器处理
  }
}

/** 格式化文件大小 */
function formatSize(bytes?: number): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  loadTasks().then(loadData)
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="所属任务">
          <el-select v-model="query.taskId" placeholder="全部任务" clearable style="width: 180px" @change="loadData">
            <el-option v-for="t in tasks" :key="t.id" :label="t.taskName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件状态">
          <el-select v-model="query.fileStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="待处理" value="PENDING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已跳过" value="SKIPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件名">
          <el-input v-model="query.fileName" placeholder="输入文件名搜索" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourcePath" label="文件路径" min-width="260" show-overflow-tooltip />
        <el-table-column prop="fileStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="fileStatusType(row.fileStatus)" size="small">
              {{ fileStatusText(row.fileStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="源文件大小" width="110">
          <template #default="{ row }">{{ formatSize(row.sourceSize) }}</template>
        </el-table-column>
        <el-table-column label="目标大小" width="100">
          <template #default="{ row }">{{ formatSize(row.targetSize) }}</template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="90" align="center" />
        <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="记录时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.fileStatus === 'FAILED'"
              type="danger"
              link
              size="small"
              @click="handleRetry(row as SyncTaskFile)"
            >
              重试
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
      <div v-if="!loading && tableData.length === 0" class="empty-hint">
        暂无文件记录
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}
</style>
