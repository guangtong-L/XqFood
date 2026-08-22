import { http } from '../utils/request'

export interface Recipe {
  id: string
  title: string
  coverUrl?: string
  cookMinutes?: number
  difficulty?: number
  stageTags?: string[]
  nutrition?: Record<string, number>
  steps?: Array<{ step: number; desc: string; timer?: number }>
  description?: string
}

export interface Ingredient {
  id: string
  name: string
  category: string
  allergenTags?: string[]
}

export interface RecipeDetail {
  recipe: Recipe
  ingredients: Ingredient[]
  recipeIngredients: Array<{ ingredientId: string; quantity: string; optional: boolean }>
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export const recipeApi = {
  list(params?: { stageTag?: string; keyword?: string; page?: number; size?: number }) {
    return http.get<PageResponse<Recipe>>('/api/v1/recipes', params as Record<string, unknown>)
  },
  detail(id: string | number) {
    return http.get<RecipeDetail>(`/api/v1/recipes/${id}`)
  }
}
