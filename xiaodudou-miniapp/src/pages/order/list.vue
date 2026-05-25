<template>
  <view class="page">
    <view v-if="loading && records.length === 0" class="loading">加载中...</view>

    <view v-else-if="records.length === 0" class="empty">
      <view class="empty-emoji">📋</view>
      <view class="empty-title">还没有订单</view>
      <button class="btn-go" @tap="goVip">去开通会员</button>
    </view>

    <view v-else class="list">
      <view v-for="o in records" :key="o.id" class="order-card" @tap="goDetail(o.id)">
        <view class="card-head">
          <text class="pkg-name">{{ o.packageName }}</text>
          <text class="status" :class="'st-' + o.status.toLowerCase()">{{ statusText(o.status) }}</text>
        </view>
        <view class="card-body">
          <view class="row"><text class="k">订单号</text><text class="v">{{ o.outTradeNo }}</text></view>
          <view class="row"><text class="k">金额</text><text class="v amt">¥{{ (o.amountFen / 100).toFixed(2) }}</text></view>
          <view class="row"><text class="k">有效期</text><text class="v">{{ o.validDays }} 天</text></view>
          <view class="row"><text class="k">下单时间</text><text class="v">{{ formatTime(o.createdAt) }}</text></view>
          <view class="row" v-if="o.paidAt"><text class="k">支付时间</text><text class="v">{{ formatTime(o.paidAt) }}</text></view>
        </view>
        <view class="card-actions" v-if="o.status === 'PAID' && canRefund(o)">
          <button class="btn-refund" @tap.stop="doRefund(o)">申请退款</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { orderApi, type Order } from '../../api/order'
import { feedback } from '../../utils/feedback'

const records = ref<Order[]>([])
const loading = ref(false)

const STATUS_TEXT: Record<string, string> = {
  PENDING: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
  EXPIRED: '已过期'
}
function statusText(s: string) { return STATUS_TEXT[s] || s }

async function load() {
  loading.value = true
  try {
    const data = await orderApi.myList({ page: 1, size: 30 })
    records.value = data.records || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function canRefund(o: Order) {
  if (!o.paidAt) return false
  const paid = new Date(o.paidAt.replace(' ', 'T'))
  const sevenDaysLater = new Date(paid.getTime() + 7 * 86400 * 1000)
  return new Date() < sevenDaysLater
}

async function doRefund(o: Order) {
  const ok = await feedback.confirm('确认申请退款？退款后会员权益将立即失效')
  if (!ok) return
  try {
    await orderApi.refund(o.id, '用户主动申请')
    feedback.success('退款成功')
    load()
  } catch (e) { console.error(e) }
}

function formatTime(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').substring(0, 16)
}

function goDetail(id: string) { /* M2 详情页 */ feedback.toast('详情页 M2 上线') }
function goVip() { uni.navigateTo({ url: '/pages/vip/center' }) }

onShow(load)
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; background: #F7F7F7; min-height: 100vh; }
.loading, .empty { padding: 160rpx 32rpx; text-align: center; color: #999; }
.empty {
  .empty-emoji { font-size: 160rpx; opacity: 0.5; }
  .empty-title { font-size: 30rpx; margin-top: 32rpx; }
  .btn-go { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; margin-top: 48rpx; width: 60%; }
}

.list .order-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 24rpx;
  .card-head { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16rpx; border-bottom: 1rpx solid #f5f5f5;
    .pkg-name { font-size: 30rpx; font-weight: 600; }
    .status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 16rpx;
      &.st-pending { color: #FF8800; background: #FFF4E5; }
      &.st-paid { color: #4CAF50; background: #E8F5E9; }
      &.st-cancelled, &.st-expired { color: #999; background: #f5f5f5; }
      &.st-refunded { color: #FF6644; background: #FFE5DC; }
    }
  }
  .card-body { padding-top: 16rpx;
    .row { display: flex; justify-content: space-between; padding: 6rpx 0; font-size: 24rpx;
      .k { color: #999; }
      .v { color: #333; max-width: 70%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
        &.amt { color: #FF6644; font-weight: 500; }
      }
    }
  }
  .card-actions { padding-top: 16rpx; border-top: 1rpx solid #f5f5f5; text-align: right;
    .btn-refund { display: inline-block; background: #fff; color: #FF6644; border: 1rpx solid #FFE5DC;
      font-size: 24rpx; height: 56rpx; line-height: 56rpx; padding: 0 24rpx; border-radius: 28rpx;
    }
  }
}
</style>
