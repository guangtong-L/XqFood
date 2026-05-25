<template>
  <view class="page">
    <!-- 头部 -->
    <view class="hero">
      <view class="hero-emoji">💎</view>
      <view class="hero-title">小肚兜会员</view>
      <view class="hero-sub" v-if="!isVip">解锁 AI 50 次/日 · 营养师咨询 · 全部菜谱</view>
      <view class="hero-sub" v-else>当前会员到期：{{ formatDate(userStore.user?.vipExpireAt) }}</view>
    </view>

    <!-- 套餐卡 -->
    <view class="pkg-list" v-if="packages.length">
      <view v-for="(p, idx) in packages" :key="p.code"
            class="pkg-card" :class="{ active: selected === p.code, hot: idx === 0 }"
            @tap="selected = p.code">
        <view v-if="idx === 0" class="hot-tag">🔥 推荐</view>
        <view class="pkg-name">{{ p.name }}</view>
        <view class="pkg-price">
          <text class="yuan">¥</text>
          <text class="amount">{{ p.amountYuan.toFixed(0) }}</text>
        </view>
        <view class="pkg-days">{{ p.validDays }} 天</view>
        <view class="pkg-benefits">{{ p.benefits }}</view>
      </view>
    </view>
    <view v-else class="loading">加载套餐中...</view>

    <!-- 权益对比 -->
    <view class="rights">
      <view class="rights-title">✨ 会员权益</view>
      <view class="rights-row" v-for="r in rights" :key="r.k">
        <text class="r-name">{{ r.name }}</text>
        <view class="r-cmp">
          <view class="r-free">免费<text class="v">{{ r.free }}</text></view>
          <view class="r-vip">会员<text class="v">{{ r.vip }}</text></view>
        </view>
      </view>
    </view>

    <!-- 底部支付栏 -->
    <view class="footer-bar" v-if="selected">
      <view class="amt">¥{{ selectedAmount }}</view>
      <button class="btn-pay" :disabled="paying" @tap="doPay">
        {{ paying ? '处理中...' : (isVip ? '续费 / 升级' : '立即开通') }}
      </button>
    </view>

    <!-- 协议提示 -->
    <view class="legal">
      开通即视为同意
      <text class="link" @tap="openLegal('terms')">《用户协议》</text>
      <text class="link" @tap="openLegal('privacy')">《隐私政策》</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { orderApi, type VipPackageItem } from '../../api/order'
import { useUserStore } from '../../store/user'
import { feedback } from '../../utils/feedback'

const userStore = useUserStore()
const packages = ref<VipPackageItem[]>([])
const selected = ref<string>('')
const paying = ref(false)

const isVip = computed(() => (userStore.user?.vipLevel ?? 0) > 0)
const selectedAmount = computed(() => {
  const p = packages.value.find(x => x.code === selected.value)
  return p ? p.amountYuan.toFixed(0) : '0'
})

const rights = [
  { k: 'ai', name: 'AI 调用', free: '5 次/日', vip: '50 次/日' },
  { k: 'consult', name: '营养师咨询', free: '无', vip: '每月 3 次' },
  { k: 'recipe', name: '全部菜谱', free: '基础', vip: '解锁 300+' },
  { k: 'family', name: '家人协作', free: '无', vip: '可邀 2 人' }
]

async function load() {
  try {
    const list = await orderApi.packages()
    packages.value = list || []
    if (!selected.value && list?.length) selected.value = list[0].code
  } catch (e) { console.error(e) }
}

async function doPay() {
  if (!selected.value) { feedback.toast('请选择套餐'); return }
  if (!userStore.isLoggedIn) {
    feedback.toast('请先登录')
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/wx-login' }), 600)
    return
  }

  paying.value = true
  try {
    const resp = await orderApi.create(selected.value)
    // Mock 模式：后端已直接 PAID，刷新用户态
    if (resp.status === 'PAID') {
      await userStore.loadMe()
      feedback.success('开通成功 🎉')
      setTimeout(() => uni.navigateBack(), 800)
    } else if (resp.status === 'PENDING') {
      // 真实模式：会有 wxPayParams，调起微信支付
      feedback.toast('待 M2 真实微信支付接入')
    }
  } catch (e) { console.error(e) }
  finally { paying.value = false }
}

function openLegal(type: 'terms' | 'privacy') {
  uni.navigateTo({ url: `/pages/legal/${type}` })
}

function formatDate(s?: string) {
  if (!s) return ''
  return s.split('T')[0]
}

onShow(load)
</script>

<style lang="scss" scoped>
.page { background: #F7F7F7; min-height: 100vh; padding: 32rpx; padding-bottom: 240rpx; }

.hero { background: linear-gradient(135deg, #FF8866, #FFB199); border-radius: 24rpx; padding: 48rpx 32rpx; text-align: center; color: #fff; margin-bottom: 32rpx;
  .hero-emoji { font-size: 96rpx; }
  .hero-title { font-size: 40rpx; font-weight: 600; margin-top: 16rpx; }
  .hero-sub { font-size: 26rpx; opacity: 0.9; margin-top: 12rpx; }
}

.pkg-list { display: flex; gap: 16rpx; margin-bottom: 32rpx; }
.pkg-card { flex: 1; background: #fff; border-radius: 24rpx; padding: 32rpx 16rpx; text-align: center; position: relative;
  border: 4rpx solid #fff; transition: all .2s;
  &.active { border-color: #FF8866; background: #FFF8F5; transform: translateY(-4rpx); }
  .hot-tag { position: absolute; top: -16rpx; left: 50%; transform: translateX(-50%);
    background: #FF6644; color: #fff; padding: 4rpx 16rpx; border-radius: 16rpx; font-size: 20rpx;
  }
  .pkg-name { font-size: 28rpx; font-weight: 600; }
  .pkg-price { margin: 16rpx 0;
    .yuan { font-size: 24rpx; color: #FF6644; }
    .amount { font-size: 56rpx; font-weight: bold; color: #FF6644; }
  }
  .pkg-days { font-size: 22rpx; color: #999; }
  .pkg-benefits { font-size: 22rpx; color: #666; margin-top: 16rpx; line-height: 1.5; min-height: 60rpx; }
}

.loading { padding: 80rpx 0; text-align: center; color: #999; }

.rights { background: #fff; border-radius: 24rpx; padding: 32rpx 24rpx; margin-bottom: 32rpx;
  .rights-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; }
  .rights-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .r-name { width: 180rpx; font-size: 26rpx; color: #333; }
    .r-cmp { flex: 1; display: flex; gap: 16rpx;
      .r-free, .r-vip { flex: 1; font-size: 22rpx; color: #999;
        .v { font-size: 24rpx; color: #333; margin-left: 8rpx; }
      }
      .r-vip .v { color: #FF6644; font-weight: 500; }
    }
  }
}

.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff;
  padding: 24rpx 32rpx; padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  display: flex; align-items: center; gap: 24rpx; box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.05);
  .amt { font-size: 40rpx; font-weight: bold; color: #FF6644; flex: 1; }
  .btn-pay { background: #FF8866; color: #fff; padding: 0 64rpx; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 30rpx;
    &[disabled] { opacity: 0.6; }
  }
}

.legal { text-align: center; color: #999; font-size: 22rpx; padding: 24rpx 32rpx 64rpx;
  .link { color: #FF8866; margin: 0 4rpx; }
}
</style>
