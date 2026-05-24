import { http, API_BASE_URL, TOKEN_STORAGE_KEY } from '../utils/request'

export interface RecognizedIngredient {
  name: string
  category: string
  quantityEstimate: string
  confidence: number
  emoji?: string
}

export interface AiRecommendation {
  recipeId: string
  title: string
  coverUrl?: string
  matchScore: number
  reason: string
  nutrition?: Record<string, number>
  cookMinutes?: number
  stageTags?: string[]
  missingIngredients?: Array<{ name: string; quantity: string }>
}

export const aiApi = {
  /** 食材识别 - 上传图片（multipart） */
  recognize(filePath: string, stageHint?: string): Promise<{ ingredients: RecognizedIngredient[]; requestId: string }> {
    const token = uni.getStorageSync(TOKEN_STORAGE_KEY)
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: API_BASE_URL + '/api/v1/ai/recognize',
        filePath,
        name: 'image',
        formData: stageHint ? { stageHint } : {},
        header: token ? { 'x-token': token } : {},
        success: (res) => {
          try {
            const body = JSON.parse(res.data)
            if (body.code === 0) resolve(body.data)
            else { uni.showToast({ title: body.message, icon: 'none' }); reject(body) }
          } catch (e) { reject(e) }
        },
        fail: (e) => { uni.showToast({ title: '上传失败', icon: 'none' }); reject(e) }
      })
    })
  },

  /** 菜谱推荐 */
  recommend(params: {
    stage: Record<string, unknown>
    ingredients: Array<{ name: string; quantity?: string }>
    constraints?: { allergies?: string[]; dislikes?: string[]; maxCookMinutes?: number }
    count?: number
  }) {
    return http.post<{ recommendations: AiRecommendation[]; disclaimer: string }>(
      '/api/v1/ai/recommend',
      params as unknown as Record<string, unknown>
    )
  }
}
