import { http } from '../utils/request'

export interface WxLoginResp {
  token: string
  userId: string
  nickname: string
  avatarUrl?: string
  vipLevel: number
}

export const authApi = {
  /** M1 Mock 登录：随便传个 code 即可 */
  wxLogin(params: { code: string; nickname?: string; avatarUrl?: string }) {
    return http.post<WxLoginResp>('/api/v1/auth/wx-login', params)
  },
  logout() {
    return http.post('/api/v1/auth/logout')
  }
}
