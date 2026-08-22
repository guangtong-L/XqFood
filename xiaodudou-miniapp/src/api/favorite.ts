import { http } from '../utils/request'
import type { PageResponse, Recipe } from './recipe'

export const favoriteApi = {
  add(recipeId: string | number) {
    return http.post(`/api/v1/favorites/${recipeId}`)
  },
  remove(recipeId: string | number) {
    return http.delete(`/api/v1/favorites/${recipeId}`)
  },
  check(recipeId: string | number) {
    return http.get<boolean>(`/api/v1/favorites/${recipeId}/check`)
  },
  myList(page = 1, size = 20) {
    return http.get<PageResponse<Recipe>>('/api/v1/favorites', { page, size })
  }
}
