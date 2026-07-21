<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listStorageSources,
  createStorageSource,
  updateStorageSource,
  deleteStorageSource,
  testConnection,
} from '@/api/storageSource'
import type { StorageSource, StorageSourceCreateDTO, StorageSourceUpdateDTO } from '@/types/api'

const loading = ref(false)
const tableData = ref<StorageSource[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增存储源')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const testingId = ref<number | null>(null)

interface FormData extends StorageSourceCreateDTO {
  id?: number
}
const formData = ref<FormData>({
  sourceName: '',
  sourceType: 'MINIO',
  endpoint: '',
  accessKey: '',
  secretKey: '',
  bucket: '',
  region: '',
  prefixPath: '',
  enabled: true,
})

const rules: FormRules = {
  sourceName: [{ required: true, message: '请输入存储源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择存储类型', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入端点地址', trigger: 'blur' }],
  accessKey: [{ required: true, message: '请输入 AccessKey', trigger: 'blur' }],
  secretKey: [{ required: true, message: '请输入 SecretKey', trigger: 'blur' }],
  bucket: [{ required: true, message: '请输入 Bucket 名称', trigger: 'blur' }],
}

/** 加载列表 */
async function loadData() {
  loading.value = true
  try {
    tableData.value = await listStorageSources()
  } finally {
    loading.value = false
  }
}

/** 重置表单 */
function resetForm() {
  formData.value = {
    sourceName: '',
    sourceType: 'MINIO',
    endpoint: '',
    accessKey: '',
    secretKey: '',
    bucket: '',
    region: '',
    prefixPath: '',
    enabled: true,
  }
}

/** 打开新增弹窗 */
function handleCreate() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增存储源'
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function handleEdit(row: StorageSource) {
  isEdit.value = true
  dialogTitle.value = '编辑存储源'
  formData.value = {
    id: row.id,
    sourceName: row.sourceName,
    sourceType: row.sourceType,
    endpoint: row.endpoint,
    accessKey: row.accessKey,
    secretKey: row.secretKey,
    bucket: row.bucket,
    region: row.region,
    prefixPath: row.prefixPath,
    enabled: row.enabled,
  }
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && formData.value.id) {
      const updateData: StorageSourceUpdateDTO = {
        sourceName: formData.value.sourceName,
        endpoint: formData.value.endpoint,
        accessKey: formData.value.accessKey,
        secretKey: formData.value.secretKey,
        bucket: formData.value.bucket,
        region: formData.value.region,
        prefixPath: formData.value.prefixPath,
        enabled: formData.value.enabled,
      }
      await updateStorageSource(formData.value.id, updateData)
      ElMessage.success('修改成功')
    } else {
      await createStorageSource(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

/** 删除 */
async function handleDelete(row: StorageSource) {
  await ElMessageBox.confirm(`确定删除存储源「${row.sourceName}」吗？`, '提示', {
    type: 'warning',
  })
  await deleteStorageSource(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/** 启用/禁用切换 */
async function handleToggleEnabled(row: StorageSource) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await updateStorageSource(row.id, { enabled: !row.enabled })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    // 错误已由拦截器处理
  }
}

/** 测试连通性 */
async function handleTestConnection(row: StorageSource) {
  testingId.value = row.id
  try {
    const result = await testConnection(row.id)
    if (result) {
      ElMessage.success('连接成功！')
    } else {
      ElMessage.error('连接失败，请检查配置')
    }
  } catch (e: any) {
    const msg = e?.message || e?.toString() || '连接测试失败'
    ElMessage.error(msg)
  } finally {
    testingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增存储源
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="sourceName" label="名称" min-width="140" />
        <el-table-column prop="sourceType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.sourceType === 'MINIO' ? 'primary' : 'success'" size="small">
              {{ row.sourceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="端点" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bucket" label="Bucket" width="120" show-overflow-tooltip />
        <el-table-column prop="prefixPath" label="前缀路径" width="120" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="handleToggleEnabled(row as StorageSource)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row as StorageSource)">编辑</el-button>
            <el-button
              type="warning"
              link
              size="small"
              :loading="testingId === row.id"
              @click="handleTestConnection(row as StorageSource)"
            >
              测试连接
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row as StorageSource)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="存储源名称" prop="sourceName">
          <el-input v-model="formData.sourceName" placeholder="如：生产环境 MinIO" />
        </el-form-item>
        <el-form-item label="存储类型" prop="sourceType">
          <el-select v-model="formData.sourceType" style="width: 100%">
            <el-option label="MinIO" value="MINIO" />
            <el-option label="华为 OBS" value="OBS" />
          </el-select>
        </el-form-item>
        <el-form-item label="端点地址" prop="endpoint">
          <el-input v-model="formData.endpoint" placeholder="S3 API地址，如 http://192.168.1.100:9000（不是控制台9001端口）" />
        </el-form-item>
        <el-form-item label="AccessKey" prop="accessKey">
          <el-input v-model="formData.accessKey" placeholder="AccessKey" />
        </el-form-item>
        <el-form-item label="SecretKey" prop="secretKey">
          <el-input v-model="formData.secretKey" type="password" placeholder="SecretKey" show-password />
        </el-form-item>
        <el-form-item label="Bucket" prop="bucket">
          <el-input v-model="formData.bucket" placeholder="Bucket 名称" />
        </el-form-item>
        <el-form-item label="Region">
          <el-input v-model="formData.region" placeholder="区域（OBS 必填）" />
        </el-form-item>
        <el-form-item label="前缀路径">
          <el-input v-model="formData.prefixPath" placeholder="如 images/，仅同步该目录下文件" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 0;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
