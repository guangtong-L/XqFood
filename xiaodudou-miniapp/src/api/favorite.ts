import { http } from '../utils/request'
import type { Recipe } from './recipe'

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
  myList() {
    return http.get<Recipe[]>('/api/v1/favorites')
  }
}
