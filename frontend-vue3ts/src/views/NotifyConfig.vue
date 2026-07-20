<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listNotifyConfigs,
  createNotifyConfig,
  updateNotifyConfig,
  deleteNotifyConfig,
} from '@/api/notifyConfig'
import type { NotifyConfig, NotifyConfigCreateDTO, NotifyConfigUpdateDTO } from '@/types/api'

const loading = ref(false)
const tableData = ref<NotifyConfig[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增通知配置')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

interface FormData extends NotifyConfigCreateDTO {
  id?: number
}
const formData = ref<FormData>({
  configName: '',
  notifyType: 'DINGTALK',
  webhookUrl: '',
  secret: '',
  enabled: true,
})

const rules: FormRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  notifyType: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  webhookUrl: [
    { required: true, message: '请输入 Webhook 地址', trigger: 'blur' },
    { type: 'url', message: '请输入正确的 URL', trigger: 'blur' },
  ],
}

/** 加载列表 */
async function loadData() {
  loading.value = true
  try {
    tableData.value = await listNotifyConfigs()
  } finally {
    loading.value = false
  }
}

/** 重置表单 */
function resetForm() {
  formData.value = {
    configName: '',
    notifyType: 'DINGTALK',
    webhookUrl: '',
    secret: '',
    enabled: true,
  }
}

/** 打开新增弹窗 */
function handleCreate() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '新增通知配置'
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function handleEdit(row: NotifyConfig) {
  isEdit.value = true
  dialogTitle.value = '编辑通知配置'
  formData.value = {
    id: row.id,
    configName: row.configName,
    notifyType: row.notifyType,
    webhookUrl: row.webhookUrl,
    secret: row.secret,
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
      const updateData: NotifyConfigUpdateDTO = {
        configName: formData.value.configName,
        webhookUrl: formData.value.webhookUrl,
        secret: formData.value.secret,
        enabled: formData.value.enabled,
      }
      await updateNotifyConfig(formData.value.id, updateData)
      ElMessage.success('修改成功')
    } else {
      await createNotifyConfig(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

/** 删除 */
async function handleDelete(row: NotifyConfig) {
  await ElMessageBox.confirm(`确定删除配置「${row.configName}」吗？`, '提示', {
    type: 'warning',
  })
  await deleteNotifyConfig(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/** 启用/禁用切换 */
async function handleToggleEnabled(row: NotifyConfig) {
  const action = row.enabled ? '禁用' : '启用'
  try {
    await updateNotifyConfig(row.id, { enabled: !row.enabled })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    // 错误已由拦截器处理
  }
}

/** 通知类型标签颜色 */
function notifyTypeTagType(row: NotifyConfig): 'info' | 'success' {
  return row.notifyType === 'DINGTALK' ? 'info' : 'success'
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <!-- 磁盘告警阈值提示 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #title>
        磁盘三级预警阈值：<el-tag size="small" type="warning">80%（提示）</el-tag>
        <el-tag size="small" type="warning" style="margin-left: 8px">90%（严重）</el-tag>
        <el-tag size="small" type="danger" style="margin-left: 8px">95%（紧急）</el-tag>
        &nbsp; （后端硬编码，待后续支持配置化）
      </template>
    </el-alert>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增通知配置
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="configName" label="配置名称" min-width="140" />
        <el-table-column prop="notifyType" label="通知类型" width="110">
          <template #default="{ row }">
            <el-tag :type="notifyTypeTagType(row as NotifyConfig)" size="small">
              {{ row.notifyType === 'DINGTALK' ? '钉钉' : '企业微信' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="webhookUrl" label="Webhook 地址" min-width="280" show-overflow-tooltip />
        <el-table-column prop="secret" label="加签密钥" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.notifyType === 'DINGTALK' ? (row.secret ? '******' : '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="(row as NotifyConfig).enabled"
              @change="handleToggleEnabled(row as NotifyConfig)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row as NotifyConfig)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row as NotifyConfig)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && tableData.length === 0" class="empty-hint">
        暂无通知配置，请点击"新增通知配置"添加
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="formData.configName" placeholder="如：运维告警群" />
        </el-form-item>
        <el-form-item label="通知类型" prop="notifyType">
          <el-select v-model="formData.notifyType" style="width: 100%">
            <el-option label="钉钉机器人" value="DINGTALK" />
            <el-option label="企业微信机器人" value="WECOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="Webhook URL" prop="webhookUrl">
          <el-input v-model="formData.webhookUrl" placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxx" />
        </el-form-item>
        <el-form-item label="加签密钥" v-if="formData.notifyType === 'DINGTALK'">
          <el-input v-model="formData.secret" placeholder="钉钉机器人加签 secret（选填）" />
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

.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}
</style>
