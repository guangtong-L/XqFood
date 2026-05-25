import { http } from '../utils/request'

export interface VipPackageItem {
  code: string
  name: string
  amountFen: number
  amountYuan: number
  vipLevel: number
  validDays: number
  benefits: string
}

export interface Order {
  id: string
  outTradeNo: string
  packageCode: string
  packageName: string
  amountFen: number
  vipLevel: number
  validDays: number
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED'
  payChannel?: string
  paidAt?: string
  createdAt: string
}

export interface PayResponse {
  orderId: string
  outTradeNo: string
  amountFen: number
  amountYuan: number
  status: string
  payChannel?: string
  expireAt: string
}

export const orderApi = {
  packages() {
    return http.get<VipPackageItem[]>('/api/v1/packages')
  },
  create(packageCode: string) {
    return http.post<PayResponse>('/api/v1/orders', { packageCode })
  },
  myList(params?: { page?: number; size?: number }) {
    return http.get<{ records: Order[]; total: number }>('/api/v1/orders', params as Record<string, unknown>)
  },
  detail(id: string | number) {
    return http.get<Order>(`/api/v1/orders/${id}`)
  },
  refund(id: string | number, reason: string) {
    return http.post(`/api/v1/orders/${id}/refund`, { reason })
  }
}
