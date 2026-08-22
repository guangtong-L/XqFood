<template>
  <view class="page">
    <swiper class="swiper" :indicator-dots="true" :autoplay="false"
            indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#fff"
            @change="onChange">
      <swiper-item v-for="(s, idx) in slides" :key="idx">
        <view class="slide" :style="{ background: s.bg }">
          <view class="emoji">{{ s.emoji }}</view>
          <view class="title">{{ s.title }}</view>
          <view class="desc">{{ s.desc }}</view>
        </view>
      </swiper-item>
    </swiper>

    <view class="footer">
      <button v-if="current < slides.length - 1" class="btn-skip" @tap="skip">跳过</button>
      <button class="btn-go" @tap="next">
        {{ current === slides.length - 1 ? '立即体验' : '下一步' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const current = ref(0)
const slides = [
  {
    emoji: '🤱',
    title: '浏览现有菜谱',
    desc: '按阶段标签查看上架菜谱；标签仅作信息筛选，不代表专业营养结论',
    bg: 'linear-gradient(135deg, #FF8866, #FFB199)'
  },
  {
    emoji: '⭐',
    title: '收藏常用菜谱',
    desc: '登录后保存喜欢的菜谱，网络失败会明确提示并支持重试',
    bg: 'linear-gradient(135deg, #FFB199, #FFD2C2)'
  },
  {
    emoji: '📅',
    title: '记录每一餐，看见成长',
    desc: '只有明确确认餐次和份数后才记录；估算仅基于已记录菜谱，不代表全天摄入',
    bg: 'linear-gradient(135deg, #FF8866, #FF6644)'
  }
]

function onChange(e: any) {
  current.value = e.detail.current
}

function next() {
  if (current.value < slides.length - 1) {
    current.value++
  } else {
    skip()
  }
}

function skip() {
  uni.setStorageSync('onboarding_done', '1')
  uni.reLaunch({ url: '/pages/auth/wx-login' })
}
</script>

<style lang="scss" scoped>
.page { width: 100%; height: 100vh; display: flex; flex-direction: column; }
.swiper { flex: 1; }
.slide { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; padding: 0 64rpx;
  .emoji { font-size: 240rpx; margin-bottom: 64rpx; }
  .title { font-size: 48rpx; font-weight: 600; }
  .desc { font-size: 28rpx; opacity: 0.9; margin-top: 24rpx; text-align: center; line-height: 1.6; }
}
.footer { padding: 32rpx; padding-bottom: calc(48rpx + env(safe-area-inset-bottom)); display: flex; gap: 24rpx; background: #fff;
  .btn-skip { flex: 1; background: #F7F7F7; color: #666; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; }
  .btn-go { flex: 2; background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 30rpx; }
}
</style>
