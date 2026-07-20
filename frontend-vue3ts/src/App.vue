<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const isLoginPage = computed(() => route.name === 'Login')

// 刷新页面后恢复用户状态（角色、昵称等）
onMounted(() => {
  if (!isLoginPage.value && userStore.token && !userStore.username) {
    userStore.fetchCurrentUser()
  }
})
</script>

<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout-container">
    <!-- 左侧深色菜单栏 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span class="logo-text">文件同步系统</span>
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
        <el-menu-item index="/storage-sources">
          <el-icon><Coin /></el-icon>
          <span>存储源管理</span>
        </el-menu-item>
        <el-menu-item index="/sync-tasks">
          <el-icon><Operation /></el-icon>
          <span>同步任务</span>
        </el-menu-item>
        <el-menu-item index="/file-records">
          <el-icon><Document /></el-icon>
          <span>文件记录</span>
        </el-menu-item>
        <el-menu-item index="/file-browser">
          <el-icon><FolderOpened /></el-icon>
          <span>文件浏览</span>
        </el-menu-item>
        <el-menu-item index="/notify-configs">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <!-- 仅管理员可见 -->
        <el-menu-item v-if="userStore.isAdmin" index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
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
          <el-button type="danger" text size="small" @click="userStore.doLogout">
            退出登录
          </el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
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
