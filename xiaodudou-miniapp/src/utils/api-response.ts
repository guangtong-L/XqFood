export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

export class ApiError extends Error {
  readonly statusCode?: number
  readonly code?: number

  constructor(message: string, statusCode?: number, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.statusCode = statusCode
    this.code = code
  }
}

export function parseApiBody<T>(raw: unknown): ApiResult<T> | null {
  let body = raw
  if (typeof body === 'string') {
    try { body = JSON.parse(body) } catch { return null }
  }
  if (!body || typeof body !== 'object' || typeof (body as ApiResult<T>).code !== 'number') return null
  return body as ApiResult<T>
}

/** 纯函数，确保 HTTP 非 2xx 与业务错误同时保留后端 code/message。 */
export function evaluateApiResponse<T>(statusCode: number, raw: unknown): T {
  const body = parseApiBody<T>(raw)
  if (statusCode < 200 || statusCode >= 300) {
    throw new ApiError(body?.message || `服务请求失败（HTTP ${statusCode}）`, statusCode, body?.code)
  }
  if (!body) throw new ApiError('服务返回格式异常', statusCode)
  if (body.code !== 0) throw new ApiError(body.message || '请求失败', statusCode, body.code)
  return body.data
}
