<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Upload, Download, FolderOpened, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { browseFiles, downloadBatch, uploadFiles, getDownloadUrl, getPreviewUrl } from '@/api/file'
import { useUserStore } from '@/stores/user'
import type { FileBrowseVO } from '@/types/api'

// ==================== 状态 ====================

const userStore = useUserStore()
const loading = ref(false)
const searchKeyword = ref('')
const currentPath = ref('')
const fileList = ref<FileBrowseVO[]>([])
const selectedRows = ref<FileBrowseVO[]>([])
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const batchDownloading = ref(false)

// 上传相关
const pendingFiles = ref<File[]>([])
const dropActive = ref(false)

// 图片预览（使用 el-dialog）
const previewVisible = ref(false)
const previewCurrentUrl = ref('')
const previewCurrentName = ref('')
const previewImages = ref<FileBrowseVO[]>([])
const previewIndex = ref(0)

// ==================== 面包屑 ====================

const breadcrumbs = computed(() => {
  if (!currentPath.value) return [{ label: '根目录', path: '' }]
  const parts = currentPath.value.split('/')
  const crumbs = [{ label: '根目录', path: '' }]
  let acc = ''
  for (const part of parts) {
    acc = acc ? acc + '/' + part : part
    crumbs.push({ label: part, path: acc })
  }
  return crumbs
})

// ==================== 文件列表过滤 ====================

const filteredFiles = computed(() => {
  if (!searchKeyword.value) return fileList.value
  const kw = searchKeyword.value.toLowerCase()
  return fileList.value.filter((f) => f.name.toLowerCase().includes(kw))
})

// ==================== 选中的非目录文件 ====================

const selectableFiles = computed(() => selectedRows.value.filter((r) => !r.isDirectory))

// ==================== 数据加载 ====================

async function loadFiles() {
  loading.value = true
  try {
    fileList.value = await browseFiles(currentPath.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadFiles())

// ==================== 目录导航 ====================

function enterDir(dir: FileBrowseVO) {
  if (!dir.isDirectory) return
  currentPath.value = dir.path
  selectedRows.value = []
  searchKeyword.value = ''
  loadFiles()
}

function goToBreadcrumb(path: string) {
  currentPath.value = path
  selectedRows.value = []
  searchKeyword.value = ''
  loadFiles()
}

// ==================== 下载 ====================

function handleDownload(file: FileBrowseVO) {
  const url = getDownloadUrl(file.path)
  const a = document.createElement('a')
  a.href = url
  a.download = file.name
  a.click()
}

async function handleBatchDownload() {
  if (selectableFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  batchDownloading.value = true
  try {
    const paths = selectableFiles.value.map((f) => f.path)
    const blob = await downloadBatch(paths)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'batch-download.zip'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('下载完成')
  } finally {
    batchDownloading.value = false
  }
}

// ==================== 预览 ====================

function handlePreview(file: FileBrowseVO) {
  previewImages.value = fileList.value.filter((f) => f.isImage)
  previewIndex.value = previewImages.value.findIndex((f) => f.path === file.path)
  updatePreview()
  previewVisible.value = true
}

function updatePreview() {
  const img = previewImages.value[previewIndex.value]
  if (img) {
    previewCurrentUrl.value = getPreviewUrl(img.path)
    previewCurrentName.value = img.name
  }
}

function prevPreview() {
  if (previewIndex.value > 0) {
    previewIndex.value--
    updatePreview()
  }
}

function nextPreview() {
  if (previewIndex.value < previewImages.value.length - 1) {
    previewIndex.value++
    updatePreview()
  }
}

// ==================== 上传 ====================

function handleFileChange(_file: any, fileList: any[]) {
  pendingFiles.value = fileList.map((f: any) => f.raw as File)
}

function handleDrop(e: DragEvent) {
  dropActive.value = false
  const files = e.dataTransfer?.files
  if (files) {
    pendingFiles.value = Array.from(files)
  }
}

async function startUpload() {
  if (pendingFiles.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    await uploadFiles(currentPath.value, pendingFiles.value)
    ElMessage.success(`成功上传 ${pendingFiles.value.length} 个文件`)
    uploadDialogVisible.value = false
    pendingFiles.value = []
    loadFiles()
  } finally {
    uploading.value = false
  }
}

function openUploadDialog() {
  pendingFiles.value = []
  uploadDialogVisible.value = true
}

// ==================== 表格选择（el-table 的 selection-change 回调参数类型为 any[]） ====================

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows as FileBrowseVO[]
}

// ==================== 格式化 ====================

function formatSize(bytes: number): string {
  if (bytes === 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let idx = 0
  let size = bytes
  while (size >= 1024 && idx < units.length - 1) {
    size /= 1024
    idx++
  }
  return size.toFixed(1) + ' ' + units[idx]
}
</script>

<template>
  <div class="file-browser">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="current-path-label">当前位置：</span>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(crumb, idx) in breadcrumbs" :key="idx">
            <a
              v-if="idx < breadcrumbs.length - 1"
              class="breadcrumb-link"
              @click.prevent="goToBreadcrumb(crumb.path)"
            >
              <el-icon><FolderOpened /></el-icon>
              {{ crumb.label }}
            </a>
            <span v-else style="display: inline-flex; align-items: center; gap: 4px">
              <el-icon><FolderOpened /></el-icon>
              {{ crumb.label }}
            </span>
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名..."
          :prefix-icon="Search"
          clearable
          style="width: 220px"
        />
        <el-button :icon="Refresh" @click="loadFiles">刷新</el-button>
        <!-- 仅管理员显示下载按钮 -->
        <template v-if="userStore.isAdmin">
          <el-button
            type="warning"
            :icon="Download"
            :loading="batchDownloading"
            :disabled="selectableFiles.length === 0"
            @click="handleBatchDownload"
          >
            批量下载 {{ selectableFiles.length > 0 ? `(${selectableFiles.length})` : '' }}
          </el-button>
        </template>
        <el-button type="primary" :icon="Upload" @click="openUploadDialog">
          上传文件
        </el-button>
      </div>
    </div>

    <!-- 文件列表 -->
    <el-table
      :data="filteredFiles"
      v-loading="loading"
      stripe
      highlight-current-row
      style="width: 100%"
      row-class-name="file-row"
      @row-dblclick="enterDir"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" :selectable="(row: any) => !row.isDirectory" />
      <el-table-column label="文件名" min-width="280">
        <template #default="{ row }">
          <div class="file-name-cell" :class="{ 'is-dir': row.isDirectory }">
            <img
              v-if="!row.isDirectory && row.isImage"
              :src="getPreviewUrl(row.path)"
              class="thumbnail"
              alt="thumb"
            />
            <el-icon v-else-if="row.isDirectory" color="#409EFF" :size="22"><FolderOpened /></el-icon>
            <el-icon v-else color="#909399" :size="20"><Document /></el-icon>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="120" align="right">
        <template #default="{ row }">
          {{ row.isDirectory ? '-' : formatSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="180">
        <template #default="{ row }">{{ row.lastModified }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.isDirectory">
            <el-button v-if="row.isImage" type="primary" text size="small" @click="handlePreview(row)">
              预览
            </el-button>
            <!-- 仅管理员显示下载按钮 -->
            <el-button v-if="userStore.isAdmin" type="success" text size="small" @click="handleDownload(row)">
              下载
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="!loading && filteredFiles.length === 0" description="目录为空" />

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="520px" :close-on-click-modal="false">
      <div
        class="upload-area"
        :class="{ 'drop-active': dropActive }"
        @dragenter.prevent="dropActive = true"
        @dragover.prevent="dropActive = true"
        @dragleave.prevent="dropActive = false"
        @drop.prevent="handleDrop"
      >
        <el-icon :size="48" color="#c0c4cc"><Upload /></el-icon>
        <p>将图片拖拽到此区域，或点击下方按钮选择</p>
        <p class="upload-hint">仅支持 jpg / png / gif / webp / bmp，单文件最大 50MB</p>
        <el-upload
          :auto-upload="false"
          multiple
          drag
          accept=".jpg,.jpeg,.png,.gif,.webp,.bmp"
          :limit="20"
          style="margin-top: 12px"
          @change="handleFileChange"
        >
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="startUpload">
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewCurrentName"
      width="80%"
      top="5vh"
      :close-on-click-modal="true"
      @closed="previewVisible = false"
    >
      <div class="preview-wrapper">
        <el-button
          class="preview-nav preview-prev"
          :icon="ArrowLeft"
          :disabled="previewIndex <= 0"
          circle
          @click="prevPreview"
        />
        <img :src="previewCurrentUrl" class="preview-img" alt="preview" />
        <el-button
          class="preview-nav preview-next"
          :icon="ArrowRight"
          :disabled="previewIndex >= previewImages.length - 1"
          circle
          @click="nextPreview"
        />
      </div>
      <div class="preview-info">
        {{ previewIndex + 1 }} / {{ previewImages.length }}
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.file-browser {
  padding: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-path-label {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.breadcrumb-link {
  color: #409EFF;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.breadcrumb-link:hover {
  color: #66b1ff;
}

.file-row {
  cursor: pointer;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name-cell.is-dir {
  color: #409EFF;
  font-weight: 500;
}

.thumbnail {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid #ebeef5;
}

/* 上传区域 */
.upload-area {
  text-align: center;
  padding: 12px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  transition: border-color 0.3s;
}
.upload-area.drop-active {
  border-color: #409EFF;
  background: #ecf5ff;
}
.upload-hint {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
}

/* 预览 */
.preview-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.preview-img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-nav {
  flex-shrink: 0;
}

.preview-info {
  text-align: center;
  margin-top: 12px;
  color: #909399;
}
</style>
