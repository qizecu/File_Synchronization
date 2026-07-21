import { get, post } from '@/utils/http'
import type { SyncTask, SyncTaskFile, SyncTaskQuery, SyncTaskFileQuery, PageResult, FileRecordVO, FileRecordQuery } from '@/types/api'

/** 任务分页列表 */
export function listSyncTasks(params?: SyncTaskQuery): Promise<PageResult<SyncTask>> {
  return get<PageResult<SyncTask>>('/sync-tasks', params)
}

/** 单条任务详情 */
export function getSyncTaskDetail(id: number): Promise<SyncTask> {
  return get<SyncTask>(`/sync-tasks/${id}`)
}

/** 快速查询任务状态 */
export function getSyncTaskStatus(id: number): Promise<string> {
  return get<string>(`/sync-tasks/${id}/status`)
}

/** 任务文件明细（分页） */
export function listTaskFiles(id: number, params?: SyncTaskFileQuery): Promise<PageResult<SyncTaskFile>> {
  return get<PageResult<SyncTaskFile>>(`/sync-tasks/${id}/files`, params)
}

/** 触发全量同步，返回 { taskId } */
export function triggerFullSync(sourceId: number): Promise<{ taskId: number }> {
  return post<{ taskId: number }>(`/sync-tasks/trigger/full/${sourceId}`)
}

/** 触发增量同步，返回 { taskId } */
export function triggerIncrementalSync(sourceId: number): Promise<{ taskId: number }> {
  return post<{ taskId: number }>(`/sync-tasks/trigger/incremental/${sourceId}`)
}

/** 重试失败文件 */
export function retryFile(fileId: number): Promise<boolean> {
  return post<boolean>(`/sync-tasks/files/${fileId}/retry`)
}

/** 文件记录查询（分页） */
export function listFileRecords(params?: FileRecordQuery): Promise<PageResult<FileRecordVO>> {
  return get<PageResult<FileRecordVO>>('/file-records', params)
}
