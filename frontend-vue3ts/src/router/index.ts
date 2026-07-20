import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', noAuth: true },
    },
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { title: 'Dashboard', icon: 'Odometer' },
    },
    {
      path: '/storage-sources',
      name: 'StorageSource',
      component: () => import('@/views/StorageSource.vue'),
      meta: { title: '存储源管理', icon: 'Coin' },
    },
    {
      path: '/sync-tasks',
      name: 'SyncTask',
      component: () => import('@/views/SyncTask.vue'),
      meta: { title: '同步任务', icon: 'Operation' },
    },
    {
      path: '/file-records',
      name: 'FileRecord',
      component: () => import('@/views/FileRecord.vue'),
      meta: { title: '文件记录', icon: 'Document' },
    },
    {
      path: '/file-browser',
      name: 'FileBrowser',
      component: () => import('@/views/FileBrowser.vue'),
      meta: { title: '文件浏览', icon: 'FolderOpened' },
    },
    {
      path: '/notify-configs',
      name: 'NotifyConfig',
      component: () => import('@/views/NotifyConfig.vue'),
      meta: { title: '系统配置', icon: 'Setting' },
    },
    {
      path: '/users',
      name: 'UserManage',
      component: () => import('@/views/UserManage.vue'),
      meta: { title: '用户管理', icon: 'UserFilled' },
    },
  ],
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.noAuth) {
    // 已登录则跳转首页
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
