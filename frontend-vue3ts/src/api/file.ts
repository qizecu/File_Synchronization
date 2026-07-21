import { get, post, del } from '@/utils/http'
import type { FileBrowseVO, GrantAccessDTO, UserFileAccessVO } from '@/types/api'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

/** 浏览目录 */
export function browseFiles(path?: string): Promise<FileBrowseVO[]> {
  return get<FileBrowseVO[]>('/files/browse', { path: path || '' })
}

/** 批量下载（返回 blob） */
export function downloadBatch(paths: string[]): Promise<Blob> {
  return post<Blob>('/files/download-batch', { paths }, { responseType: 'blob' })
}

/** 单文件下载 URL（直接用 a 标签下载，避免大文件 blob 内存问题） */
export function getDownloadUrl(path: string): string {
  const token = localStorage.getItem('token') || ''
  return `${API_BASE}/files/download?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
}

/** 图片预览 URL */
export function getPreviewUrl(path: string): string {
  const token = localStorage.getItem('token') || ''
  return `${API_BASE}/files/preview?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
}

/** 上传文件 */
export function uploadFiles(targetDir: string, files: File[]): Promise<string[]> {
  const formData = new FormData()
  formData.append('targetDir', targetDir)
  files.forEach((f) => formData.append('files', f))
  return post<string[]>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 上传超时 2 分钟
  })
}

/** 管理员授权用户访问文件/目录 */
export function grantAccess(data: GrantAccessDTO): Promise<void> {
  return post<void>('/files/grant-access', data)
}

/** 撤销授权 */
export function revokeAccess(id: number): Promise<void> {
  return del<void>(`/files/grant-access/${id}`)
}

/** 查看某用户的授权列表 */
export function listUserAccess(userId: number): Promise<UserFileAccessVO[]> {
  return get<UserFileAccessVO[]>(`/files/grant-access/${userId}`)
}

/** 查看所有用户的授权概要 */
export function listAllGrants(): Promise<UserFileAccessVO[]> {
  return get<UserFileAccessVO[]>('/files/grant-access/all')
}
