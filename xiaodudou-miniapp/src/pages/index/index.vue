<template>
  <view class="page">
    <!-- 未登录提示 -->
    <view v-if="!userStore.isLoggedIn" class="stage-card guest" @tap="goLogin">
      <view class="stage-emoji">👋</view>
      <view class="stage-info">
        <view class="stage-title">点击登录</view>
        <view class="stage-sub">登录后可保存收藏和已食用记录</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- 未设档案提示 -->
    <view v-else-if="!userStore.profile?.stageType" class="stage-card guest" @tap="goSetup">
      <view class="stage-emoji">📝</view>
      <view class="stage-info">
        <view class="stage-title">完善阶段信息</view>
        <view class="stage-sub">用于展示阶段信息，敏感画像可稍后填写</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- 阶段卡 -->
    <view v-else class="stage-card" @tap="goSetup">
      <view class="stage-emoji">{{ stageEmoji }}</view>
      <view class="stage-info">
        <view class="stage-title">{{ stageTitle }}</view>
        <view class="stage-sub">阶段信息仅用于筛选与展示，请结合个人情况核对</view>
      </view>
      <view class="stage-arrow">→</view>
    </view>

    <!-- AI 入口 -->
    <view v-if="AI_FEATURE_ENABLED" class="ai-entry" @tap="goCamera">
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
      <view v-if="recipeState === 'loading'" class="empty">正在加载菜谱...</view>
      <view v-else-if="recipeState === 'error'" class="state-box">菜谱加载失败，请检查网络<button @tap="loadRecipes">重试</button></view>
      <view class="meal-list" v-else-if="recipes.length">
        <view class="meal-card" v-for="r in recipes" :key="r.id" @tap="goDetail(r.id)">
          <image v-if="r.coverUrl && !failedImages.has(String(r.id))" class="meal-img" :src="r.coverUrl" mode="aspectFill" @error="markImageFailed(r.id)" />
          <view v-else class="meal-img image-fallback">🍲</view>
          <view class="meal-name">{{ r.title }}</view>
        </view>
      </view>
      <view v-else class="empty">暂无上架菜谱，可到菜谱页稍后重试</view>
    </view>

    <!-- 仅基于主动记录的营养估算 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">📋 今日已记录</text>
      </view>
      <view v-if="nutritionState === 'loading'" class="empty">正在加载记录...</view>
      <view v-else-if="nutritionState === 'error'" class="state-box">今日记录加载失败，不能据此判断为空<button @tap="loadNutrition">重试</button></view>
      <view v-else-if="!hasCheckin" class="nutrition-empty">
        <view class="ne-icon">📋</view>
        <view class="ne-text">尚未记录已完成/已食用的菜谱</view>
        <view class="ne-sub">只有您确认餐次和份数后才会计入</view>
        <view class="ne-btn" @tap="goRecipes">去查看菜谱</view>
      </view>
      <view v-else class="nutrition-list">
        <view class="record-summary">今日已记录 {{ nutrition?.recordedEntries || 0 }} 餐 / {{ nutrition?.recordedServings || 0 }} 份</view>
        <view class="estimate-title">估算营养（仅基于记录，不代表全天摄入）</view>
        <view class="estimate-grid">
          <view class="estimate-item" v-for="item in estimatedItems" :key="item.key">
            <text>{{ item.label }}</text><text>{{ item.value }} {{ item.unit }}</text>
          </view>
        </view>
        <view class="nutrition-tip">{{ nutrition?.disclaimer }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { recipeApi, type Recipe } from '../../api/recipe'
import { nutritionApi, type NutritionToday } from '../../api/nutrition'
import { feedback } from '../../utils/feedback'
import { AI_FEATURE_ENABLED } from '../../config/features'

const userStore = useUserStore()
const recipes = ref<Recipe[]>([])
const recipeState = ref<'loading' | 'ready' | 'error'>('loading')
const nutritionState = ref<'loading' | 'ready' | 'error'>('loading')
const failedImages = ref(new Set<string>())

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
const nutrition = ref<NutritionToday | null>(null)
const hasCheckin = computed(() => (nutrition.value?.recordedEntries || 0) > 0)
const nutrientMeta: Record<string, { label: string; unit: string }> = {
  calories: { label: '热量', unit: 'kcal' }, protein: { label: '蛋白质', unit: 'g' },
  calcium: { label: '钙', unit: 'mg' }, iron: { label: '铁', unit: 'mg' },
  vitA: { label: '维A', unit: 'μg' }, vitC: { label: '维C', unit: 'mg' }
}
const estimatedItems = computed(() => Object.entries(nutrition.value?.estimatedNutrition || {}).map(([key, value]) => ({
  key, value, label: nutrientMeta[key]?.label || key, unit: nutrientMeta[key]?.unit || ''
})))

async function loadNutrition() {
  if (!userStore.isLoggedIn) {
    nutrition.value = null
    nutritionState.value = 'ready'
    return
  }
  nutritionState.value = 'loading'
  try {
    nutrition.value = await nutritionApi.today()
    nutritionState.value = 'ready'
  } catch {
    nutritionState.value = 'error'
  }
}

async function loadRecipes() {
  recipeState.value = 'loading'
  try {
    const tag = userStore.profile?.stageType === 'POSTPARTUM' ? 'lactation' :
                userStore.profile?.stageType === 'WEANING' ? 'weaning' : ''
    const data = await recipeApi.list({ stageTag: tag, size: 3, page: 1 })
    recipes.value = data.records
    recipeState.value = 'ready'
  } catch (e) {
    recipeState.value = 'error'
  }
}

function markImageFailed(id: string | number) {
  failedImages.value = new Set(failedImages.value).add(String(id))
}

onShow(async () => {
  if (userStore.isLoggedIn && !userStore.user) {
    await userStore.loadMe()
  }
  await loadRecipes()
  await loadNutrition()
})

function goLogin() { uni.navigateTo({ url: '/pages/auth/wx-login' }) }
function goSetup() { uni.navigateTo({ url: '/pages/profile/setup' }) }
function goCamera() { uni.navigateTo({ url: '/pages/camera/index' }) }
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
.state-box { text-align: center; color: #777; padding: 32rpx 0; font-size: 25rpx;
  button { margin-top: 20rpx; width: 220rpx; min-height: 80rpx; border-radius: 40rpx; background: #FFF0EB; color: #D94F2B; font-size: 25rpx; }
}
.meal-list { display: flex; gap: 16rpx;
  .meal-card { flex: 1; background: #FFF8F5; border-radius: 12rpx; overflow: hidden;
    .meal-img { width: 100%; height: 160rpx; background: #eee; }
    .image-fallback { display: flex; align-items: center; justify-content: center; font-size: 52rpx; color: #aaa; }
    .meal-name { padding: 16rpx; font-size: 24rpx; text-align: center; }
  }
}
.nutrition-list {
  .record-summary { font-size: 30rpx; font-weight: 600; color: #333; }
  .estimate-title { font-size: 24rpx; color: #666; margin-top: 20rpx; }
  .estimate-grid { margin-top: 12rpx; display: grid; grid-template-columns: 1fr 1fr; gap: 12rpx;
    .estimate-item { display: flex; justify-content: space-between; padding: 14rpx; background: #FFF8F5; border-radius: 10rpx; font-size: 23rpx; }
  }
  .nutrition-tip { font-size: 21rpx; color: #999; line-height: 1.6; margin-top: 18rpx; }
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
