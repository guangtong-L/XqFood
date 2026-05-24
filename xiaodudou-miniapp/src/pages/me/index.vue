<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="not-login">
      <view class="emoji">👋</view>
      <view class="text">登录后享受 AI 个性化推荐</view>
      <button class="btn-login" @tap="goLogin">立即登录</button>
    </view>

    <template v-else>
      <view class="profile">
        <image v-if="userStore.user?.avatarUrl" :src="userStore.user.avatarUrl" class="avatar-img" />
        <view v-else class="avatar">👩</view>
        <view class="name">{{ userStore.user?.nickname || '小肚兜妈妈' }}</view>
        <view class="stage" @tap="goSetup">{{ stageLabel }} ›</view>
      </view>

      <view class="vip-card">
        <view class="vip-info">
          <view class="vip-title">{{ vipTitle }}</view>
          <view class="vip-sub">{{ vipSub }}</view>
        </view>
        <button class="btn-renew">{{ userStore.user?.vipLevel ? '续费' : '开通' }}</button>
      </view>

      <view class="menu">
        <view class="menu-item" @tap="goSetup"><text>📋 我的档案</text><text class="arrow">›</text></view>
        <view class="menu-item" @tap="goFavorites">
          <text>⭐ 收藏菜谱</text>
          <text class="arrow">{{ favCount > 0 ? favCount : '' }} ›</text>
        </view>
        <view class="menu-item" @tap="goCheckin"><text>📅 打卡日历</text><text class="arrow">›</text></view>
        <view class="menu-item"><text>📊 营养报告 <text class="soon">即将上线</text></text><text class="arrow">›</text></view>
        <view class="menu-item"><text>👨‍👩‍👧 家人协作 <text class="soon">即将上线</text></text><text class="arrow">›</text></view>
        <view class="menu-item"><text>💬 营养师咨询 <text class="soon">即将上线</text></text><text class="arrow">›</text></view>
        <view class="menu-item"><text>🛒 我的订单 <text class="soon">即将上线</text></text><text class="arrow">›</text></view>
        <view class="menu-item"><text>⚙️ 设置 <text class="soon">即将上线</text></text><text class="arrow">›</text></view>
        <view class="menu-item logout" @tap="doLogout"><text>🚪 退出登录</text></view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { authApi } from '../../api/auth'
import { favoriteApi } from '../../api/favorite'

const userStore = useUserStore()
const favCount = ref(0)

async function loadFavCount() {
  if (!userStore.isLoggedIn) { favCount.value = 0; return }
  try {
    const list = await favoriteApi.myList()
    favCount.value = list?.length || 0
  } catch { favCount.value = 0 }
}

onShow(async () => {
  if (userStore.isLoggedIn && !userStore.user) {
    await userStore.loadMe()
  }
  loadFavCount()
})

function goFavorites() {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/me/favorites' })
}
function goCheckin() {
  if (!userStore.isLoggedIn) return
  uni.navigateTo({ url: '/pages/me/checkin' })
}

const stageLabel = computed(() => {
  const p = userStore.profile
  if (!p) return '点击完善阶段画像'
  if (p.stageType === 'PREPARE') return '🤍 备孕中'
  if (p.stageType === 'PREGNANCY') return `🤰 孕 ${p.pregnancyWeek ?? '?'} 周`
  if (p.stageType === 'POSTPARTUM') return `🤱 月子第 ${p.postpartumDay ?? '?'} 天`
  if (p.stageType === 'WEANING') return '🍼 辅食期'
  if (p.stageType === 'CHILD') return '🧒 儿童餐'
  return '阶段未设置'
})

const vipTitle = computed(() => {
  const lv = userStore.user?.vipLevel ?? 0
  if (lv === 0) return '💎 开通会员，解锁全部 AI 能力'
  if (lv === 1) return '💎 月子精养卡'
  if (lv === 2) return '💎 辅食年卡'
  if (lv === 3) return '💎 儿童年卡'
  return '💎 会员中'
})

const vipSub = computed(() => {
  const lv = userStore.user?.vipLevel ?? 0
  return lv === 0 ? '每天 3 次免费 AI 推荐' : '无限 AI 推荐 + 专家咨询次数'
})

function goLogin() {
  uni.navigateTo({ url: '/pages/auth/wx-login' })
}

function goSetup() {
  uni.navigateTo({ url: '/pages/profile/setup' })
}

async function doLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号？',
    success: async (res) => {
      if (res.confirm) {
        try { await authApi.logout() } catch {}
        userStore.logout()
        uni.reLaunch({ url: '/pages/auth/wx-login' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; min-height: 100vh; background: #F7F7F7; }

.not-login { padding-top: 200rpx; text-align: center;
  .emoji { font-size: 120rpx; }
  .text { font-size: 28rpx; color: #999; margin: 32rpx 0; }
  .btn-login { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; width: 60%; }
}

.profile {
  background: #fff; border-radius: 24rpx; padding: 48rpx 32rpx;
  text-align: center; margin-bottom: 24rpx;
  .avatar { font-size: 96rpx; }
  .avatar-img { width: 128rpx; height: 128rpx; border-radius: 50%; }
  .name { font-size: 32rpx; font-weight: 600; margin-top: 16rpx; }
  .stage { font-size: 26rpx; color: #FF8866; margin-top: 8rpx; }
}
.vip-card {
  background: linear-gradient(135deg, #FFE5DC, #FFD2C2);
  border-radius: 24rpx; padding: 32rpx;
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24rpx;
  .vip-info { flex: 1; }
  .vip-title { font-size: 30rpx; font-weight: 600; }
  .vip-sub { font-size: 22rpx; color: #666; margin-top: 8rpx; }
  .btn-renew {
    background: #FF8866; color: #fff;
    font-size: 26rpx; padding: 16rpx 32rpx;
    border-radius: 32rpx; line-height: 1; margin-left: 16rpx;
  }
}
.menu {
  background: #fff; border-radius: 24rpx; overflow: hidden;
  .menu-item {
    padding: 32rpx; font-size: 28rpx;
    display: flex; justify-content: space-between; align-items: center;
    border-bottom: 1rpx solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .arrow { color: #ccc; font-size: 24rpx; }
    .soon { font-size: 20rpx; color: #ccc; margin-left: 12rpx; padding: 2rpx 12rpx; background: #f5f5f5; border-radius: 8rpx; }
    &.logout { color: #FF6644; justify-content: center; }
  }
}
</style>
