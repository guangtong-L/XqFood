import { http } from '../utils/request'

export interface FeedItem {
  actionId: string
  checkedAt: string
  displayName: string       // "小**" 脱敏
  avatarUrl?: string
  stageType: string
  stageDesc: string         // "月子第 12 天"
  recipeId: string
  recipeTitle: string
  recipeCover?: string
}

export interface CommunityFeed {
  stageType: string
  activeUsers: number       // 同阶段活跃妈妈数
  records: FeedItem[]
  page: number
  size: number
}

export const communityApi = {
  /** 同阶段打卡流，不传 stageType 默认用当前用户阶段 */
  feed(params?: { stageType?: string; page?: number; size?: number }) {
    return http.get<CommunityFeed>('/api/v1/community/feed', params as Record<string, unknown>)
  }
}
