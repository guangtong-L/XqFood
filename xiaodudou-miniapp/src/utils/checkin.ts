export const MEAL_TYPES = ['breakfast', 'lunch', 'dinner', 'snack'] as const
export type MealType = typeof MEAL_TYPES[number]

export function formatLocalDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function localDateDaysAgo(days: number, now = new Date()) {
  const date = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  date.setDate(date.getDate() - days)
  return formatLocalDate(date)
}

export function validateServings(value: number) {
  return Number.isFinite(value) && value >= 0.25 && value <= 10
    && Math.abs(value * 100 - Math.round(value * 100)) < 1e-8
}

export function buildCheckinInput(recipeId: string | number, mealType: MealType, servings: number, actionDate?: string) {
  if (!MEAL_TYPES.includes(mealType)) throw new Error('请选择餐次')
  if (!validateServings(servings)) throw new Error('份数必须在 0.25–10 之间，最多两位小数')
  return { recipeId, mealType, servings, ...(actionDate ? { actionDate } : {}) }
}
