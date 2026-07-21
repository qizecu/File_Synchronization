<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { listFileRecords, retryFile } from '@/api/syncTask'
import { getUserList } from '@/api/user'
import { getPreviewUrl, getDownloadUrl } from '@/api/file'
import type { FileRecordVO, SysUserVO } from '@/types/api'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref<FileRecordVO[]>([])
const total = ref(0)
const userList = ref<SysUserVO[]>([])

const activeTab = ref<'ALL' | 'SYNC' | 'UPLOAD'>('ALL')

const query = reactive({
  page: 1,
  size: 15,
  userId: undefined as number | undefined,
  status: '' as string,
  fileName: '',
})

/** 加载用户列表（仅 ADMIN） */
async function loadUsers() {
  if (!userStore.isAdmin) return
  try {
    const res = await getUserList({ page: 1, size: 200 })
    userList.value = res.records
  } catch {
    // ignore
  }
}

/** 加载文件记录 */
async function loadData() {
  loading.value = true
  try {
    const type = activeTab.value === 'ALL' ? undefined : activeTab.value
    const res = await listFileRecords({
      page: query.page,
      size: query.size,
      type,
      userId: userStore.isAdmin ? query.userId : undefined,
      status: query.status || undefined,
      fileName: query.fileName || undefined,
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  query.page = 1
  loadData()
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

function handleSearch() {
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

/** 来源文本 */
function originText(origin: string): string {
  return origin === 'UPLOAD' ? '上传' : '同步'
}

/** 用户显示 */
function userDisplay(row: FileRecordVO): string {
  if (row.fileOrigin === 'SYNC') {
    return row.userNickname || '系统'
  }
  return row.userNickname || '-'
}

/** 格式化文件大小 */
function formatSize(bytes?: number | null): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

/** 重试失败文件 */
async function handleRetry(row: FileRecordVO) {
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

/** 预览 */
function handlePreview(row: FileRecordVO) {
  window.open(getPreviewUrl(row.fileName), '_blank')
}

/** 下载 */
function handleDownload(row: FileRecordVO) {
  const url = getDownloadUrl(row.fileName)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName.split('/').pop() || row.fileName
  a.click()
}

/** 显示的用户列 */
const showUserColumn = computed(() => userStore.isAdmin)

onMounted(() => {
  loadUsers()
  loadData()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <!-- Tab 切换 -->
      <el-radio-group v-model="activeTab" size="default" @change="handleTabChange" style="margin-bottom: 12px">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="SYNC">同步记录</el-radio-button>
        <el-radio-button value="UPLOAD">上传记录</el-radio-button>
      </el-radio-group>

      <el-form :inline="true" :model="query" size="default" style="margin-top: 8px">
        <el-form-item v-if="showUserColumn" label="用户">
          <el-select v-model="query.userId" placeholder="全部" clearable style="width: 150px" @change="handleSearch">
            <el-option label="全部" :value="undefined" />
            <el-option label="系统" :value="-1" />
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="handleSearch">
            <el-option label="待处理" value="PENDING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已跳过" value="SKIPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件名">
          <el-input
            v-model="query.fileName"
            placeholder="输入文件名搜索"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column label="来源" width="70">
          <template #default="{ row }">
            <el-tag :type="row.fileOrigin === 'UPLOAD' ? 'success' : ''" size="small">
              {{ originText(row.fileOrigin) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="showUserColumn" label="用户" width="100">
          <template #default="{ row }">{{ userDisplay(row) }}</template>
        </el-table-column>
        <el-table-column label="存储源" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sourceName || '-' }}</template>
        </el-table-column>
        <el-table-column label="文件大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="fileStatusType(row.fileStatus)" size="small">
              {{ fileStatusText(row.fileStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
            <el-button
              v-if="row.fileStatus === 'FAILED'"
              type="danger"
              link
              size="small"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
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
