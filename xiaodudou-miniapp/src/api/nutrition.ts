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

export interface NutritionReportItem {
  key: NutritionItem['key']
  name: string
  unit: string
  avgActual: number
  target: number
  avgPercent: number
}

export interface NutritionReport {
  stageType: string
  stageLabel: string
  days: number
  startDate: string
  endDate: string
  checkinCount: number
  checkinDays: number
  avgActual: Record<string, number>
  target: Record<string, number>
  avgPercent: Record<string, number>
  items: NutritionReportItem[]
  topRecipes: Array<{ recipeId: number | string; title: string; count: number }>
}

export const nutritionApi = {
  today() {
    return http.get<NutritionToday>('/api/v1/nutrition/today')
  },
  report(days: 1 | 7 | 30 = 7) {
    return http.get<NutritionReport>('/api/v1/nutrition/report', { days })
  }
}
