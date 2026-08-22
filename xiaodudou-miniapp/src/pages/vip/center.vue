<template>
  <view class="unavailable-page">
    <view class="emoji">🛡️</view>
    <view class="title">会员服务暂未开放</view>
    <view class="description">购买与支付能力仍在安全验收中，当前不能选择套餐或发起订单。</view>
    <button class="back" @tap="goBack">返回</button>
  </view>
</template>

<script setup lang="ts">
function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
.unavailable-page { min-height: 100vh; padding: 200rpx 56rpx 80rpx; box-sizing: border-box; text-align: center; background: #F7F7F7; }
.emoji { font-size: 120rpx; }
.title { margin-top: 32rpx; font-size: 36rpx; font-weight: 600; color: #333; }
.description { margin-top: 20rpx; font-size: 26rpx; line-height: 1.7; color: #777; }
.back { margin-top: 56rpx; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; background: #FF8866; color: #fff; font-size: 28rpx; }
</style>
