<template>
  <view class="page">
    <!-- 未登录提示 -->
    <view v-if="!userStore.isLoggedIn" class="stage-card guest" @tap="goLogin">
      <view class="stage-emoji">👋</view>
      <view class="stage-info">
        <view class="stage-title">点击登录</view>
        <view class="stage-sub">登录后享 AI 个性化推荐</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- 未设档案提示 -->
    <view v-else-if="!userStore.profile?.stageType" class="stage-card guest" @tap="goSetup">
      <view class="stage-emoji">📝</view>
      <view class="stage-info">
        <view class="stage-title">完善阶段信息</view>
        <view class="stage-sub">完善后 AI 才能为你个性化</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- 阶段卡 -->
    <view v-else class="stage-card" @tap="goSetup">
      <view class="stage-emoji">{{ stageEmoji }}</view>
      <view class="stage-info">
        <view class="stage-title">{{ stageTitle }}</view>
        <view class="stage-sub">今日重点：{{ stageFocus }}</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- AI 入口 -->
    <view class="ai-entry" @tap="goCamera">
      <view class="ai-icon">📸</view>
      <view class="ai-text">
        <view class="ai-title">拍冰箱，AI 给你做菜</view>
        <view class="ai-sub">立即拍照 →</view>
      </view>
    </view>

    <!-- 今日推荐 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">📅 推荐菜谱</text>
        <text class="section-link" @tap="goRecipes">更多</text>
      </view>
      <view class="meal-list" v-if="recipes.length">
        <view class="meal-card" v-for="r in recipes" :key="r.id" @tap="goDetail(r.id)">
          <image class="meal-img" :src="r.coverUrl" mode="aspectFill" />
          <view class="meal-name">{{ r.title }}</view>
        </view>
      </view>
      <view v-else class="empty">加载中...</view>
    </view>

    <!-- 营养雷达 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">📊 本周营养</text>
        <text v-if="hasCheckin" class="section-link">详情</text>
      </view>

      <!-- 未打卡引导 -->
      <view v-if="!hasCheckin" class="nutrition-empty">
        <view class="ne-icon">📋</view>
        <view class="ne-text">完成今日打卡后，AI 自动为你计算</view>
        <view class="ne-sub">连续打卡 3 天解锁专属营养报告</view>
        <view class="ne-btn" @tap="goCheckin">开始打卡</view>
      </view>

      <!-- 有打卡数据时（M2 接真实数据） -->
      <view v-else class="nutrition-list">
        <view class="nutrition-item" v-for="n in nutrition" :key="n.name">
          <view class="nutrition-name">{{ n.name }}</view>
          <view class="nutrition-bar">
            <view class="nutrition-bar-inner" :style="{ width: n.percent + '%' }"></view>
          </view>
          <view class="nutrition-percent">{{ n.percent }}%</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { recipeApi, type Recipe } from '../../api/recipe'
import { checkinApi } from '../../api/checkin'
import { feedback } from '../../utils/feedback'

const userStore = useUserStore()
const recipes = ref<Recipe[]>([])

const stageEmoji = computed(() => {
  const t = userStore.profile?.stageType
  if (t === 'PREPARE') return '🤍'
  if (t === 'PREGNANCY') return '🤰'
  if (t === 'POSTPARTUM') return '🤱'
  if (t === 'WEANING') return '🍼'
  if (t === 'CHILD') return '🧒'
  return '🌱'
})
const stageTitle = computed(() => {
  const p = userStore.profile
  if (!p) return '未设置'
  if (p.stageType === 'POSTPARTUM') return `月子第 ${p.postpartumDay ?? '?'} 天`
  if (p.stageType === 'PREGNANCY') return `孕 ${p.pregnancyWeek ?? '?'} 周`
  if (p.stageType === 'PREPARE') return '备孕中'
  if (p.stageType === 'WEANING') return '辅食期'
  if (p.stageType === 'CHILD') return '儿童餐'
  return p.stageType
})
const stageFocus = computed(() => {
  const t = userStore.profile?.stageType
  if (t === 'POSTPARTUM') return '催乳 + 补血'
  if (t === 'PREGNANCY') return '叶酸 + 钙铁'
  if (t === 'WEANING') return '月龄阶梯营养'
  return '均衡营养'
})

const nutrition = ref([
  { name: '蛋白质', percent: 80 },
  { name: '钙', percent: 60 },
  { name: '铁', percent: 50 }
])

// 今日是否有打卡（M2 营养雷达接真实数据时再用 nutrition 计算）
const todayCheckinCount = ref(0)
const hasCheckin = computed(() => todayCheckinCount.value > 0)

async function loadCheckinToday() {
  if (!userStore.isLoggedIn) { todayCheckinCount.value = 0; return }
  try {
    const list = await checkinApi.today()
    todayCheckinCount.value = list?.length || 0
  } catch { todayCheckinCount.value = 0 }
}

function goCheckin() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/wx-login' })
    return
  }
  uni.navigateTo({ url: '/pages/me/checkin' })
}

async function loadRecipes() {
  try {
    const tag = userStore.profile?.stageType === 'POSTPARTUM' ? 'lactation' :
                userStore.profile?.stageType === 'WEANING' ? 'weaning' : ''
    const data = await recipeApi.list({ stageTag: tag, size: 3, page: 1 })
    recipes.value = data.records
  } catch (e) { console.error(e) }
}

onShow(async () => {
  if (userStore.isLoggedIn && !userStore.user) {
    await userStore.loadMe()
  }
  await loadRecipes()
  await loadCheckinToday()
})

function goLogin() { uni.navigateTo({ url: '/pages/auth/wx-login' }) }
function goSetup() { uni.navigateTo({ url: '/pages/profile/setup' }) }
function goCamera() { uni.switchTab({ url: '/pages/camera/index' }) }
function goRecipes() { uni.switchTab({ url: '/pages/recipe/list' }) }
function goDetail(id: string) { uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` }) }
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; }

.stage-card {
  display: flex; align-items: center;
  background: linear-gradient(135deg, #FFE5DC 0%, #FFD2C2 100%);
  border-radius: 24rpx; padding: 32rpx 24rpx; margin-bottom: 24rpx;
  .stage-emoji { font-size: 64rpx; margin-right: 24rpx; }
  .stage-info { flex: 1;
    .stage-title { font-size: 32rpx; font-weight: 600; color: #333; }
    .stage-sub { font-size: 24rpx; color: #666; margin-top: 8rpx; }
  }
  .stage-arrow { font-size: 32rpx; color: #FF8866; }
  &.guest { background: linear-gradient(135deg, #f0f0f0, #fafafa); }
}

.ai-entry {
  display: flex; align-items: center;
  background: #FF8866; border-radius: 24rpx; padding: 40rpx 32rpx; margin-bottom: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 136, 102, 0.3);
  .ai-icon { font-size: 80rpx; margin-right: 24rpx; }
  .ai-text { flex: 1; color: #fff;
    .ai-title { font-size: 32rpx; font-weight: 600; }
    .ai-sub { font-size: 24rpx; margin-top: 8rpx; opacity: 0.9; }
  }
}

.section { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 24rpx;
  .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx;
    .section-title { font-size: 28rpx; font-weight: 600; }
    .section-link { font-size: 24rpx; color: #FF8866; }
  }
}
.empty { text-align: center; color: #999; padding: 48rpx 0; font-size: 26rpx; }
.meal-list { display: flex; gap: 16rpx;
  .meal-card { flex: 1; background: #FFF8F5; border-radius: 12rpx; overflow: hidden;
    .meal-img { width: 100%; height: 160rpx; background: #eee; }
    .meal-name { padding: 16rpx; font-size: 24rpx; text-align: center; }
  }
}
.nutrition-list {
  .nutrition-item { display: flex; align-items: center; margin-bottom: 16rpx;
    .nutrition-name { width: 100rpx; font-size: 26rpx; }
    .nutrition-bar { flex: 1; height: 16rpx; background: #f0f0f0; border-radius: 8rpx; overflow: hidden; margin: 0 16rpx;
      .nutrition-bar-inner { height: 100%; background: linear-gradient(90deg, #FF8866, #FFB199); }
    }
    .nutrition-percent { width: 60rpx; font-size: 24rpx; color: #999; text-align: right; }
  }
}
.nutrition-empty {
  text-align: center; padding: 24rpx 16rpx;
  .ne-icon { font-size: 80rpx; opacity: 0.5; }
  .ne-text { font-size: 26rpx; color: #666; margin-top: 16rpx; }
  .ne-sub { font-size: 22rpx; color: #999; margin-top: 8rpx; }
  .ne-btn {
    display: inline-block;
    background: #FF8866; color: #fff;
    padding: 12rpx 40rpx; border-radius: 32rpx;
    font-size: 26rpx; margin-top: 24rpx;
  }
}
</style>
