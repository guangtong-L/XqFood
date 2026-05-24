import { http } from '../utils/request'

export interface User {
  id: string
  nickname: string
  avatarUrl?: string
  vipLevel: number
  vipExpireAt?: string
}

export interface UserProfile {
  id?: string
  userId?: string
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

export const userApi = {
  me() {
    return http.get<{ user: User; profile: UserProfile | null }>('/api/v1/user/me')
  },
  getProfile() {
    return http.get<UserProfile | null>('/api/v1/user/profile')
  },
  saveProfile(profile: UserProfile) {
    return http.post<UserProfile>('/api/v1/user/profile', profile as unknown as Record<string, unknown>)
  }
}
