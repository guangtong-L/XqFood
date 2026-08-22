import { http } from '../utils/request'

export interface CheckinItem {
  actionId: string
  recipeId: string
  title: string
  coverUrl?: string
  nutrition?: Record<string, number>
  checkedAt: string
  mealType?: 'breakfast' | 'lunch' | 'dinner' | 'snack'
  servings?: number
  actionDate: string
}

export interface CalendarData {
  month: string         // "2026-05"
  daysInMonth: number
  checkinDays: number
  totalCheckins: number
  dayCount: Record<string, number>  // {"2026-05-23": 3}
}

export const checkinApi = {
  /** 打卡（完成了某个菜谱） */
  checkin(input: { recipeId: string | number; mealType: 'breakfast' | 'lunch' | 'dinner' | 'snack'; servings: number; actionDate?: string }) {
    return http.post<{ actionId: string; created: boolean; alreadyExists: boolean }>('/api/v1/checkin', input)
  },
  /** 今日打卡列表 */
  today() {
    return http.get<CheckinItem[]>('/api/v1/checkin/today')
  },
  /** 月度日历 */
  calendar(month?: string) {
    return http.get<CalendarData>('/api/v1/checkin/calendar', month ? { month } : undefined)
  },
  remove(actionId: string | number) {
    return http.delete<{ deleted: boolean }>(`/api/v1/checkin/${actionId}`)
  }
}
