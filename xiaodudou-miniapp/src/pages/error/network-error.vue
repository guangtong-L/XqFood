<template>
  <view class="page">
    <view class="emoji">📡</view>
    <view class="title">网络好像有点不稳</view>
    <view class="sub">请检查 WiFi 或移动数据后重试</view>

    <view class="tips">
      <view class="tip-title">您可以试试：</view>
      <view class="tip-item">· 切换 WiFi / 移动网络</view>
      <view class="tip-item">· 打开飞行模式后再关闭</view>
      <view class="tip-item">· 稍后再试</view>
    </view>

    <view class="actions">
      <button class="btn-primary" :loading="retrying" @tap="retry">重新加载</button>
      <button class="btn-ghost" @tap="goHome">回到首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { feedback as fb } from '../../utils/feedback'

const retrying = ref(false)

async function retry() {
  retrying.value = true
  // 简单探活：调用 health
  try {
    const res = await uni.request({ url: 'http://localhost:8080/api/v1/health', timeout: 5000 })
    if (res.statusCode === 200) {
      fb.success('网络已恢复')
      setTimeout(() => uni.navigateBack(), 600)
    } else {
      fb.error('仍连不上，请稍后再试')
    }
  } catch {
    fb.error('仍连不上，请稍后再试')
  } finally {
    retrying.value = false
  }
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh; background: #F7F7F7;
  display: flex; flex-direction: column; align-items: center;
  padding: 160rpx 48rpx 48rpx;
}
.emoji { font-size: 160rpx; margin-bottom: 32rpx; }
.title { font-size: 36rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
.sub { font-size: 26rpx; color: #999; text-align: center; margin-bottom: 48rpx; }
.tips {
  background: #fff; border-radius: 16rpx;
  padding: 24rpx 32rpx; width: 100%; max-width: 560rpx;
  margin-bottom: 48rpx;
  .tip-title { font-size: 24rpx; color: #666; margin-bottom: 12rpx; }
  .tip-item { font-size: 26rpx; color: #999; padding: 6rpx 0; }
}
.actions { width: 100%; max-width: 480rpx; }
.btn-primary {
  background: #FF8866; color: #fff;
  height: 88rpx; line-height: 88rpx;
  border-radius: 44rpx; font-size: 30rpx; margin-bottom: 24rpx;
}
.btn-ghost {
  background: #fff; color: #FF8866;
  height: 88rpx; line-height: 88rpx;
  border-radius: 44rpx; font-size: 30rpx;
  border: 1rpx solid #FFE5DC;
}
</style>
