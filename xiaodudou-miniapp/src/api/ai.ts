import { http, API_BASE_URL, TOKEN_STORAGE_KEY, processApiResponse, showRequestError, ApiError } from '../utils/request'

export interface RecognizedIngredient {
  name: string
  category: string
  quantityEstimate: string
  confidence: number
  emoji?: string
}

export interface AiRecommendation {
  recipe: {
    id: number
    title: string
    coverUrl?: string
    cookMinutes?: number
    nutrition?: Record<string, number>
    stageTags?: string[]
  }
  matchScore: number
  reason: string
  missingIngredients?: Array<{ name: string; quantity: string }>
}

interface AiResponseMeta {
  aiLabel: string
  fallback: boolean
  disclaimer: string
}

export const aiApi = {
  /** 食材识别 - 上传图片（multipart） */
  recognize(filePath: string): Promise<{ ingredients: RecognizedIngredient[]; requestId: string; modelVersion: string } & AiResponseMeta> {
    const token = uni.getStorageSync(TOKEN_STORAGE_KEY)
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: API_BASE_URL + '/api/v1/ai/recognize',
        filePath,
        name: 'image',
        header: token ? { 'x-token': token } : {},
        timeout: 30000,
        success: (res) => {
          try {
            resolve(processApiResponse(res.statusCode, res.data))
          } catch (error) {
            showRequestError(error, '上传失败')
            reject(error)
          }
        },
        fail: (error) => {
          const requestError = new ApiError(error.errMsg?.includes('timeout') ? '上传超时，请稍后重试' : '上传失败，请检查网络')
          showRequestError(requestError)
          reject(requestError)
        }
      })
    })
  },

  /** 菜谱推荐 */
  recommend(params: {
    ingredients: Array<{ name: string; quantityEstimate?: string }>
    maxCookMinutes: number
    count: number
  }) {
    return http.post<{ recommendations: AiRecommendation[]; allergyNotice: string } & AiResponseMeta>(
      '/api/v1/ai/recommend',
      params as unknown as Record<string, unknown>
    )
  }
}
