import { http } from '../utils/request'

export interface NutritionToday {
  date: string
  recordedEntries: number
  includedEntries: number
  recordedServings: number
  estimatedNutrition: Record<string, number>
  estimated: true
  basis: string
  dataQuality: 'unverified'
  disclaimer: string
}

export const nutritionApi = {
  today() {
    return http.get<NutritionToday>('/api/v1/nutrition/today')
  }
}
