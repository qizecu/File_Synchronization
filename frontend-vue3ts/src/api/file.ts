import { get, post } from '@/utils/http'
import type { FileBrowseVO } from '@/types/api'

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
  return `/api/files/download?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
}

/** 图片预览 URL */
export function getPreviewUrl(path: string): string {
  const token = localStorage.getItem('token') || ''
  return `/api/files/preview?path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
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
