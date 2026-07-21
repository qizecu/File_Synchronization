import { get, del } from '@/utils/http'

/** 查看当前所有锁 */
export function listLocks(): Promise<Array<{ key: string; sourceId: string; ttlMillis: number }>> {
  return get<Array<{ key: string; sourceId: string; ttlMillis: number }>>('/redis/locks')
}

/** 清除指定存储源的锁 */
export function deleteLock(sourceId: number): Promise<string> {
  return del<string>(`/redis/locks/${sourceId}`)
}

/** 清除所有锁 */
export function deleteAllLocks(): Promise<{ count: number; message: string }> {
  return del<{ count: number; message: string }>('/redis/locks')
}
