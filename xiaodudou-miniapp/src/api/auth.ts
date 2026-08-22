import { http, request } from '../utils/request'

export interface WxLoginResp {
  token: string
  userId: string
  nickname: string
  avatarUrl?: string
  vipLevel: number
  loginMode: 'real' | 'mock'
}

export const authApi = {
  /** 微信 code 登录；Mock 仅由后端 dev/local 显式开关控制。 */
  wxLogin(params: { code: string; nickname?: string; avatarUrl?: string }) {
    return request<WxLoginResp>({ url: '/api/v1/auth/wx-login', method: 'POST', data: params, skipAuth: true })
  },
  logout() {
    return http.post('/api/v1/auth/logout')
  }
}
