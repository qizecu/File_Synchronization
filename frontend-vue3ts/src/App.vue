<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted, ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'

const route = useRoute()
const userStore = useUserStore()
const isLoginPage = computed(() => route.name === 'Login')

// 刷新页面后恢复用户状态（角色、昵称等）
onMounted(() => {
  if (!isLoginPage.value && userStore.token && !userStore.username) {
    userStore.fetchCurrentUser()
  }
})

// ==================== 修改密码弹窗 ====================

const pwdDialogVisible = ref(false)
const pwdDialogLoading = ref(false)
const pwdFormRef = ref<any>(null)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPwd = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为8-32位', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: '密码必须包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' },
  ],
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  pwdDialogLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    userStore.doLogout()
  } finally {
    pwdDialogLoading.value = false
  }
}
</script>

<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout-container">
    <!-- 左侧深色菜单栏 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span class="logo-text">文件同步管理平台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <!-- 仅管理员可见 -->
        <template v-if="userStore.isAdmin">
          <el-menu-item index="/storage-sources">
            <el-icon><Coin /></el-icon>
            <span>存储源管理</span>
          </el-menu-item>
          <el-menu-item index="/sync-tasks">
            <el-icon><Operation /></el-icon>
            <span>同步任务</span>
          </el-menu-item>
        </template>
        <el-menu-item index="/file-records">
          <el-icon><Document /></el-icon>
          <span>文件记录</span>
        </el-menu-item>
        <el-menu-item index="/file-browser">
          <el-icon><FolderOpened /></el-icon>
          <span>文件浏览</span>
        </el-menu-item>
        <!-- 仅管理员可见 -->
        <template v-if="userStore.isAdmin">
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <el-header class="layout-header">
        <span class="header-title">{{ $route.meta.title || '文件同步系统' }}</span>
        <div class="header-right">
          <span class="header-user">
            <el-icon><User /></el-icon>
            {{ userStore.nickname || '管理员' }}
          </span>
          <el-button type="primary" text size="small" :icon="Key" @click="openPwdDialog">
            修改密码
          </el-button>
          <el-button type="danger" text size="small" @click="userStore.doLogout">
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="pwdDialogVisible"
      title="修改密码"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="8-32位，需包含字母和数字"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialogLoading" @click="handleChangePassword">
          确定
        </el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style>
/* 全局重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
}
</style>

<style scoped>
.layout-container {
  height: 100%;
}

.layout-aside {
  background-color: #001529;
  overflow-y: auto;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #002140;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
}

/* Element Plus 菜单覆盖深色风格 */
.layout-aside .el-menu {
  border-right: none;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 24px;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.layout-main {
  background: #f0f2f5;
  min-height: 0;
}
</style>
