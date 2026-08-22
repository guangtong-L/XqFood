<template>
  <view class="page">
    <view class="emoji">🍼</view>
    <view class="title">页面不见啦</view>
    <view class="sub">可能是链接失效，或者页面正在升级中</view>
    <view class="actions">
      <button class="btn-primary" @tap="goHome">回到首页</button>
      <button class="btn-ghost" @tap="goBack">返回上一页</button>
    </view>
    <view class="footer">如持续异常，请通过“帮助与反馈”提交问题</view>
  </view>
</template>

<script setup lang="ts">
function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else goHome()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh; background: #F7F7F7;
  display: flex; flex-direction: column; align-items: center;
  padding: 200rpx 48rpx 48rpx;
}
.emoji { font-size: 160rpx; margin-bottom: 32rpx; }
.title { font-size: 36rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
.sub { font-size: 26rpx; color: #999; text-align: center; margin-bottom: 64rpx; }
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
.footer { font-size: 22rpx; color: #bbb; margin-top: 80rpx; }
</style>
