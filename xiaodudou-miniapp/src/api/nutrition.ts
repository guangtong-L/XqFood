import { http } from '../utils/request'

export interface NutritionItem {
  key: 'calories' | 'protein' | 'calcium' | 'iron' | 'vitA' | 'vitC'
  name: string
  unit: string
  actual: number
  target: number
  percent: number
}

export interface NutritionToday {
  stageType: string
  stageLabel: string
  date: string
  checkinCount: number
  actual: Record<string, number>
  target: Record<string, number>
  percent: Record<string, number>
  items: NutritionItem[]
}

export const nutritionApi = {
  today() {
    return http.get<NutritionToday>('/api/v1/nutrition/today')
  }
}
