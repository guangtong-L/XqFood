/**
 * 统一请求封装：环境化地址、HTTP/业务错误、超时和登录失效采用一致策略。
 */

import { ApiError, evaluateApiResponse, parseApiBody } from './api-response'
export { ApiError } from './api-response'

const TOKEN_KEY = 'x-token'
const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim()

function resolveBaseUrl() {
  const baseUrl = (rawBaseUrl || (import.meta.env.DEV ? 'http://localhost:8080' : '')).replace(/\/$/, '')
  if (!baseUrl) throw new Error('生产构建缺少 VITE_API_BASE_URL')

  let parsed: URL
  try {
    parsed = new URL(baseUrl)
  } catch {
    throw new Error('VITE_API_BASE_URL 不是有效 URL')
  }

  if (import.meta.env.PROD) {
    const host = parsed.hostname.toLowerCase()
    if (parsed.protocol !== 'https:' || host === 'localhost' || host === '127.0.0.1') {
      throw new Error('生产 VITE_API_BASE_URL 必须使用 HTTPS 且不能指向 localhost')
    }
  }
  return baseUrl
}

const BASE_URL = resolveBaseUrl()
let redirectingToLogin = false

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown> | FormData
  header?: Record<string, string>
  timeout?: number
  skipAuth?: boolean
}


export function handleUnauthorized() {
  uni.removeStorageSync(TOKEN_KEY)
  if (redirectingToLogin) return
  redirectingToLogin = true
  uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({
      url: '/pages/auth/wx-login',
      complete: () => { redirectingToLogin = false }
    })
  }, 500)
}

export function processApiResponse<T>(statusCode: number, raw: unknown): T {
  const body = parseApiBody<T>(raw)
  if (statusCode === 401 || body?.code === 30001) {
    handleUnauthorized()
    throw new ApiError('未登录或登录已失效', statusCode, body?.code)
  }
  return evaluateApiResponse<T>(statusCode, raw)
}

export function showRequestError(error: unknown, fallback = '网络异常，请稍后重试') {
  const message = error instanceof Error
    ? (error.message.includes('timeout') ? '请求超时，请稍后重试' : error.message)
    : fallback
  if (message !== '未登录或登录已失效') uni.showToast({ title: message || fallback, icon: 'none' })
}

export function request<T = unknown>(options: RequestOptions): Promise<T> {
  const token = uni.getStorageSync(TOKEN_KEY)
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.header
  }
  if (token && !options.skipAuth) header[TOKEN_KEY] = token

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      timeout: options.timeout ?? 10000,
      success: (res) => {
        try {
          resolve(processApiResponse<T>(res.statusCode, res.data))
        } catch (error) {
          showRequestError(error)
          reject(error)
        }
      },
      fail: (error) => {
        const requestError = new ApiError(error.errMsg?.includes('timeout') ? '请求超时，请稍后重试' : '网络异常，请检查网络连接')
        showRequestError(requestError)
        reject(requestError)
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
