<template>
  <view class="page">
    <view class="icon">📊</view>
    <view class="title">营养报告暂未开放</view>
    <view class="desc">目标百分比、达标判断和多日健康建议仍在进行数据与专业审核。</view>
    <view class="desc">当前仅在首页展示基于您主动记录菜谱和份数的估算汇总，不代表全天摄入。</view>
    <button class="btn" @tap="back">返回</button>
  </view>
</template>

<script setup lang="ts">
function back() {
  uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 220rpx 56rpx; text-align: center; background: #F7F7F7; }
.icon { font-size: 112rpx; opacity: .65; }
.title { margin-top: 32rpx; font-size: 34rpx; font-weight: 600; }
.desc { margin-top: 18rpx; color: #666; font-size: 26rpx; line-height: 1.7; }
.btn { margin-top: 56rpx; background: #FF8866; color: #fff; border-radius: 44rpx; }
</style>
