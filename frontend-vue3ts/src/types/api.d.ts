/** 登录请求参数 */
export interface LoginDTO {
  username: string
  password: string
}

/** 登录响应 */
export interface LoginVO {
  token: string
  nickname: string
  username: string
  role: string // ADMIN / USER
}

/** 后端统一分页返回 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 存储源实体 */
export interface StorageSource {
  id: number
  sourceName: string
  sourceType: 'MINIO' | 'OBS'
  endpoint: string
  accessKey: string
  secretKey: string
  bucket: string
  region?: string
  prefixPath?: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

/** 存储源创建参数 */
export interface StorageSourceCreateDTO {
  sourceName: string
  sourceType: 'MINIO' | 'OBS'
  endpoint: string
  accessKey: string
  secretKey: string
  bucket: string
  region?: string
  prefixPath?: string
  enabled?: boolean
}

/** 存储源更新参数 */
export interface StorageSourceUpdateDTO {
  sourceName?: string
  endpoint?: string
  accessKey?: string
  secretKey?: string
  bucket?: string
  region?: string
  prefixPath?: string
  enabled?: boolean
}

/** 同步任务实体 */
export interface SyncTask {
  id: number
  taskName: string
  sourceName: string
  taskType: 'FULL' | 'INCREMENTAL'
  sourceId: number
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'PARTIAL'
  totalFiles: number
  successFiles: number
  failedFiles: number
  skippedFiles: number
  currentCursor?: string
  errorMsg?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
}

/** 同步任务查询参数 */
export interface SyncTaskQuery {
  page?: number
  size?: number
  taskType?: string
  status?: string
  sourceId?: number
}

/** 同步任务文件实体 */
export interface SyncTaskFile {
  id: number
  taskId: number
  sourcePath: string
  sourceMd5?: string
  sourceSize?: number
  targetPath?: string
  targetMd5?: string
  targetSize?: number
  fileStatus: 'PENDING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  retryCount: number
  errorMsg?: string
  createdAt: string
  updatedAt: string
}

/** 文件查询参数 */
export interface SyncTaskFileQuery {
  page?: number
  size?: number
  fileStatus?: string
}

/** 通知配置实体 */
export interface NotifyConfig {
  id: number
  configName: string
  notifyType: 'DINGTALK' | 'WECOM'
  webhookUrl: string
  secret?: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

/** 通知配置创建参数 */
export interface NotifyConfigCreateDTO {
  configName: string
  notifyType: 'DINGTALK' | 'WECOM'
  webhookUrl: string
  secret?: string
  enabled?: boolean
}

/** 文件浏览条目 */
export interface FileBrowseVO {
  name: string
  path: string
  size: number
  lastModified: string
  isDirectory: boolean
  extension: string
  isImage: boolean
}

/** Dashboard 统计数据 */
export interface DashboardStatsVO {
  todaySyncCount: number
  totalSyncedFiles: number
  totalStorageSize: number
  totalSources: number
  successRate: number
  diskUsage: number
  diskTotal: number
  diskUsable: number
  recentTasks: RecentTaskVO[]
}

export interface RecentTaskVO {
  id: number
  taskName: string
  taskType: string
  status: string
  totalFiles: number
  successFiles: number
  createdAt: string
}

/** 系统用户 */
export interface SysUserVO {
  id: number
  username: string
  nickname: string
  role: string
  status: number
  createdAt: string
}

/** 新增用户 */
export interface UserCreateDTO {
  username: string
  nickname: string
  role: string
}

/** 编辑用户 */
export interface UserUpdateDTO {
  nickname?: string
  role?: string
  status?: number
}

/** 重置密码参数 */
export interface ResetPasswordDTO {
  newPassword: string
}

/** 修改密码参数 */
export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
}

/** 文件授权请求 */
export interface GrantAccessDTO {
  userId: number
  filePath: string
}

/** 文件授权记录 */
export interface UserFileAccessVO {
  id: number
  userId: number
  username: string
  filePath: string
  grantedBy: number | null
  createdAt: string
}

/** 文件记录视图 */
export interface FileRecordVO {
  id: number
  fileName: string
  fileOrigin: 'SYNC' | 'UPLOAD'
  userId: number | null
  userNickname: string | null
  sourceName: string | null
  fileSize: number | null
  fileStatus: 'PENDING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  errorMsg: string | null
  createdAt: string
}

/** 文件记录查询参数 */
export interface FileRecordQuery {
  page?: number
  size?: number
  type?: 'SYNC' | 'UPLOAD' | 'ALL'
  userId?: number
  status?: 'PENDING' | 'SUCCESS' | 'FAILED'
  fileName?: string
}
