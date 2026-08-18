import { http } from '../utils/request'

export interface FeedbackItem {
  id: number | string
  content: string
  contact?: string
  category: string
  status: 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'IGNORED'
  reply?: string
  repliedAt?: string
  createdAt: string
}

export interface FeedbackSubmitReq {
  content: string
  contact?: string
  category?: 'general' | 'bug' | 'suggestion' | 'business'
  clientInfo?: Record<string, unknown>
}

export const feedbackApi = {
  submit(data: FeedbackSubmitReq) {
    return http.post<{ feedbackId: number; status: string; message: string }>('/api/v1/feedback', data as Record<string, unknown>)
  },
  myList() {
    return http.get<FeedbackItem[]>('/api/v1/feedback/my')
  }
}
