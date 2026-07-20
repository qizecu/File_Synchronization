import { get, post, put, del } from '@/utils/http'
import type { NotifyConfig, NotifyConfigCreateDTO, NotifyConfigUpdateDTO } from '@/types/api'

/** 获取全部通知配置 */
export function listNotifyConfigs(): Promise<NotifyConfig[]> {
  return get<NotifyConfig[]>('/notify-configs')
}

/** 新增通知配置 */
export function createNotifyConfig(data: NotifyConfigCreateDTO): Promise<NotifyConfig> {
  return post<NotifyConfig>('/notify-configs', data)
}

/** 修改通知配置 */
export function updateNotifyConfig(id: number, data: NotifyConfigUpdateDTO): Promise<void> {
  return put<void>(`/notify-configs/${id}`, data)
}

/** 删除通知配置 */
export function deleteNotifyConfig(id: number): Promise<void> {
  return del<void>(`/notify-configs/${id}`)
}
