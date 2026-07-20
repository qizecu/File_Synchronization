import { get, post, put, del } from '@/utils/http'
import type { StorageSource, StorageSourceCreateDTO, StorageSourceUpdateDTO } from '@/types/api'

/** 获取全部存储源 */
export function listStorageSources(): Promise<StorageSource[]> {
  return get<StorageSource[]>('/storage-sources')
}

/** 新增存储源 */
export function createStorageSource(data: StorageSourceCreateDTO): Promise<StorageSource> {
  return post<StorageSource>('/storage-sources', data)
}

/** 修改存储源 */
export function updateStorageSource(id: number, data: StorageSourceUpdateDTO): Promise<void> {
  return put<void>(`/storage-sources/${id}`, data)
}

/** 删除存储源 */
export function deleteStorageSource(id: number): Promise<void> {
  return del<void>(`/storage-sources/${id}`)
}

/** 测试存储源连通性 */
export function testConnection(id: number): Promise<boolean> {
  return post<boolean>(`/storage-sources/${id}/test`)
}
