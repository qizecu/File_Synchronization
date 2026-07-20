import { get, post } from '@/utils/http'
import type { SyncTask, SyncTaskFile, SyncTaskQuery, SyncTaskFileQuery, PageResult } from '@/types/api'

/** 任务分页列表 */
export function listSyncTasks(params?: SyncTaskQuery): Promise<PageResult<SyncTask>> {
  return get<PageResult<SyncTask>>('/sync-tasks', params)
}

/** 单条任务详情 */
export function getSyncTaskDetail(id: number): Promise<SyncTask> {
  return get<SyncTask>(`/sync-tasks/${id}`)
}

/** 任务文件明细（分页） */
export function listTaskFiles(id: number, params?: SyncTaskFileQuery): Promise<PageResult<SyncTaskFile>> {
  return get<PageResult<SyncTaskFile>>(`/sync-tasks/${id}/files`, params)
}

/** 触发全量同步 */
export function triggerFullSync(sourceId: number): Promise<void> {
  return post<void>(`/sync-tasks/trigger/full/${sourceId}`)
}

/** 触发增量同步 */
export function triggerIncrementalSync(sourceId: number): Promise<void> {
  return post<void>(`/sync-tasks/trigger/incremental/${sourceId}`)
}

/** 重试失败文件 */
export function retryFile(fileId: number): Promise<boolean> {
  return post<boolean>(`/sync-tasks/files/${fileId}/retry`)
}
