/**
 * 统一请求封装
 */

const BASE_URL = 'http://localhost:8080'
const TOKEN_KEY = 'x-token'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown> | FormData
  header?: Record<string, string>
  timeout?: number
  skipAuth?: boolean
}

export function request<T = unknown>(options: RequestOptions): Promise<T> {
  const token = uni.getStorageSync(TOKEN_KEY)
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.header
  }
  if (token && !options.skipAuth) {
    header[TOKEN_KEY] = token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      timeout: options.timeout ?? 10000,
      success: (res) => {
        const body = res.data as ApiResult<T>
        if (res.statusCode === 401 || body.code === 30001) {
          uni.removeStorageSync(TOKEN_KEY)
          uni.showToast({ title: '请先登录', icon: 'none' })
          setTimeout(() => uni.redirectTo({ url: '/pages/auth/wx-login' }), 500)
          reject(new Error('未登录'))
          return
        }
        if (body.code === 0) {
          resolve(body.data)
        } else {
          uni.showToast({ title: body.message ?? '请求失败', icon: 'none' })
          reject(new Error(body.message))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export const http = {
  get<T = unknown>(url: string, data?: Record<string, unknown>) {
    return request<T>({ url, method: 'GET', data })
  },
  post<T = unknown>(url: string, data?: Record<string, unknown>) {
    return request<T>({ url, method: 'POST', data })
  },
  put<T = unknown>(url: string, data?: Record<string, unknown>) {
    return request<T>({ url, method: 'PUT', data })
  },
  delete<T = unknown>(url: string, data?: Record<string, unknown>) {
    return request<T>({ url, method: 'DELETE', data })
  }
}

export const TOKEN_STORAGE_KEY = TOKEN_KEY
export const API_BASE_URL = BASE_URL
