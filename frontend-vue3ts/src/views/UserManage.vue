<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, resetUserPassword } from '@/api/user'
import type { SysUserVO, UserCreateDTO, UserUpdateDTO } from '@/types/api'

// ==================== 状态 ====================

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const userList = ref<SysUserVO[]>([])

// 弹窗控制
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const dialogLoading = ref(false)

// 表单
const form = reactive<UserCreateDTO & { id?: number; status?: number }>({
  username: '',
  nickname: '',
  role: 'USER',
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const formRef = ref<any>(null)

// ==================== 数据加载 ====================

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({ page: page.value, size: size.value, keyword: keyword.value })
    userList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadUsers())

function handleSearch() {
  page.value = 1
  loadUsers()
}

function handlePageChange(p: number) {
  page.value = p
  loadUsers()
}

function handleSizeChange(s: number) {
  size.value = s
  page.value = 1
  loadUsers()
}

// ==================== 新增/编辑 ====================

function openAddDialog() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  form.id = undefined
  form.username = ''
  form.nickname = ''
  form.role = 'USER'
  form.status = undefined
  dialogVisible.value = true
}

function openEditDialog(row: SysUserVO) {
  isEdit.value = true
  dialogTitle.value = `编辑用户 - ${row.username}`
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname
  form.role = row.role
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    if (isEdit.value && form.id) {
      const dto: UserUpdateDTO = {}
      if (form.nickname) dto.nickname = form.nickname
      if (form.role) dto.role = form.role
      if (form.status !== undefined) dto.status = form.status
      await updateUser(form.id, dto)
      ElMessage.success('编辑成功')
    } else {
      await createUser({
        username: form.username,
        nickname: form.nickname,
        role: form.role,
      })
      ElMessage.success('新增成功，默认密码为 123456')
    }
    dialogVisible.value = false
    loadUsers()
  } finally {
    dialogLoading.value = false
  }
}

// ==================== 状态切换（启用/禁用） ====================

async function toggleStatus(row: SysUserVO) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${label}用户 "${row.username}"？`, '提示', {
      type: 'warning',
    })
    await updateUser(row.id, { status: newStatus })
    ElMessage.success(`已${label}`)
    loadUsers()
  } catch {
    // 取消
  }
}

// ==================== 删除 ====================

async function handleDelete(row: SysUserVO) {
  try {
    await ElMessageBox.confirm(`确认删除用户 "${row.username}"？此操作不可恢复`, '警告', {
      type: 'error',
      confirmButtonText: '确认删除',
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch {
    // 取消
  }
}

// ==================== 重置密码 ====================

async function handleResetPassword(row: SysUserVO) {
  try {
    await ElMessageBox.confirm(`确认重置用户 "${row.username}" 的密码为 123456？`, '提示', {
      type: 'warning',
    })
    await resetUserPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch {
    // 取消
  }
}
</script>

<template>
  <div class="user-manage">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名 / 昵称..."
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增用户</el-button>
      </div>
    </div>

    <!-- 用户表格 -->
    <el-table :data="userList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="row.username === 'admin'"
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" :icon="Edit" @click="openEditDialog(row)">
            编辑
          </el-button>
          <el-button
            type="warning"
            text
            size="small"
            :icon="Key"
            @click="handleResetPassword(row)"
          >
            重置密码
          </el-button>
          <el-button
            type="danger"
            text
            size="small"
            :icon="Delete"
            :disabled="row.username === 'admin'"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            :disabled="isEdit"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role" v-if="!isEdit || form.username !== 'admin'">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            :disabled="form.username === 'admin'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage {
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
