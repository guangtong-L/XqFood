import { http } from '../utils/request'

export interface User {
  id: string
  nickname: string
  avatarUrl?: string
  vipLevel: number
  vipExpireAt?: string
  createdAt: string
  profile: UserProfile | null
}

export interface UserProfile {
  stageType: 'PREPARE' | 'PREGNANCY' | 'POSTPARTUM' | 'WEANING' | 'CHILD'
  pregnancyWeek?: number
  postpartumDay?: number
  deliveryType?: 'natural' | 'cesarean'
  feedingType?: 'breast' | 'mixed' | 'formula'
  babyBirthDate?: string
  allergies?: string[]
  dislikes?: string[]
  healthNotes?: string
}

export interface SaveProfileRequest extends UserProfile {
  sensitiveInfoConsent: true
}

export const userApi = {
  me() {
    return http.get<User>('/api/v1/user/me')
  },
  getProfile() {
    return http.get<UserProfile | null>('/api/v1/user/profile')
  },
  saveProfile(profile: SaveProfileRequest) {
    return http.post<UserProfile>('/api/v1/user/profile', profile as unknown as Record<string, unknown>)
  },
  deleteMe(confirmation: string) {
    return http.delete<{ deleted: boolean }>('/api/v1/user/me', { confirmation })
  }
}
