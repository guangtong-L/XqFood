<template>
  <view class="page">
    <view class="icon">🔒</view>
    <view class="title">妈妈圈暂未开放</view>
    <view class="desc">我们尚未完成独立的发布与展示同意机制，因此不会读取或展示任何用户头像、孕周、产后天数或打卡动态。</view>
    <button class="button" @tap="goHome">返回首页</button>
  </view>
</template>

<script setup lang="ts">
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped lang="scss">
.page { min-height: 100vh; box-sizing: border-box; padding: 180rpx 56rpx 60rpx; text-align: center; background: #F7F7F7; }
.icon { font-size: 112rpx; }
.title { margin-top: 32rpx; font-size: 36rpx; font-weight: 600; color: #333; }
.desc { margin-top: 24rpx; font-size: 27rpx; line-height: 1.7; color: #777; }
.button { margin-top: 56rpx; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; background: #FF8866; color: #fff; font-size: 28rpx; }
</style>
