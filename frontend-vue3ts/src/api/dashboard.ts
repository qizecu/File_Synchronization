import { get } from '@/utils/http'
import type { DashboardStatsVO } from '@/types/api'

/** 获取 Dashboard 统计数据 */
export function getDashboardStats(): Promise<DashboardStatsVO> {
  return get<DashboardStatsVO>('/dashboard/stats')
}
