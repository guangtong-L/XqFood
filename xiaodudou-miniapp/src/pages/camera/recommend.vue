<template>
  <view class="page">
    <!-- Loading -->
    <view v-if="loading" class="loading-area">
      <view class="loading-icon">🤖</view>
      <view class="loading-title">AI 正在为你挑菜</view>
      <view class="loading-sub">{{ loadingText }}</view>
      <view class="progress-bar">
        <view class="progress-inner" :style="{ width: progress + '%' }"></view>
      </view>
      <view class="loading-tip">通常 3-8 秒</view>
    </view>

    <!-- Empty -->
    <view v-else-if="recommendations.length === 0" class="empty">
      <view class="empty-emoji">🍽</view>
      <view class="empty-title">暂无推荐</view>
      <view class="empty-sub">{{ errorMsg || '请回上一步重新识别' }}</view>
      <button class="btn-retry" @tap="loadRecommend">重试</button>
      <view class="alt" @tap="goBack">← 返回</view>
    </view>

    <!-- Results -->
    <template v-else>
      <view class="hint">💡 基于现有食材 + {{ stageDesc }}</view>

      <view v-if="isFallback" class="fallback-tip">
        ⚠️ AI 服务繁忙，已切换至备用推荐
      </view>

      <view class="recipe-card" v-for="(r, idx) in recommendations" :key="r.recipeId" @tap="goDetail(r.recipeId)">
        <view class="rank">{{ ['🥇','🥈','🥉'][idx] || '🏅' }}</view>
        <view class="content">
          <view class="title">
            {{ r.title }}
            <text class="score">{{ r.matchScore }} 分</text>
          </view>
          <view class="reason">{{ r.reason }}</view>
          <view class="meta">
            <text v-if="r.cookMinutes">🕐 {{ r.cookMinutes }} 分钟</text>
            <text v-if="r.nutrition?.calories">🔥 {{ r.nutrition.calories }} kcal</text>
          </view>
          <view v-if="r.missingIngredients && r.missingIngredients.length" class="missing">
            缺料：{{ r.missingIngredients.map(m => m.name).join('、') }}
          </view>
          <view v-else class="no-missing">✓ 食材齐全</view>
          <view class="actions">
            <text class="btn-link" @tap.stop="goDetail(r.recipeId)">查看做法 →</text>
            <text class="btn-link" @tap.stop="addToToday(r.recipeId)">加入今日</text>
          </view>
        </view>
      </view>

      <view class="disclaimer">⚠️ AI 推荐仅供参考，特殊体质遵医嘱</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { aiApi, type AiRecommendation } from '../../api/ai'
import { checkinApi } from '../../api/checkin'
import { useUserStore } from '../../store/user'
import { feedback } from '../../utils/feedback'

const userStore = useUserStore()
const recommendations = ref<AiRecommendation[]>([])
const loading = ref(false)
const loadingText = ref('调用 AI 中...')
const progress = ref(0)
const errorMsg = ref('')
const isFallback = ref(false)

let progressTimer: ReturnType<typeof setInterval> | null = null

const stageDesc = computed(() => {
  const p = userStore.profile
  if (!p) return '默认阶段'
  if (p.stageType === 'POSTPARTUM') return `月子第 ${p.postpartumDay ?? '?'} 天`
  if (p.stageType === 'WEANING') return '辅食期'
  if (p.stageType === 'PREGNANCY') return `孕 ${p.pregnancyWeek ?? '?'} 周`
  return p.stageType
})

function startProgress() {
  progress.value = 0
  loadingText.value = '准备食材清单...'
  progressTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value = Math.min(90, progress.value + (progress.value < 60 ? 6 : 2))
      if (progress.value < 25) loadingText.value = '准备食材清单...'
      else if (progress.value < 55) loadingText.value = '调用 AI 营养师...'
      else if (progress.value < 80) loadingText.value = '匹配 8 道候选菜谱...'
      else loadingText.value = '排序结果...'
    }
  }, 400)
}

function stopProgress() {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
  progress.value = 100
}

async function loadRecommend() {
  errorMsg.value = ''
  isFallback.value = false
  loading.value = true
  startProgress()

  try {
    const cached = uni.getStorageSync('recommend_input')
    const params = cached || {
      stage: userStore.profile || { stageType: 'POSTPARTUM', postpartumDay: 12 },
      ingredients: [],
      constraints: {},
      count: 3
    }
    const data = await aiApi.recommend({ ...params, count: 3 })
    recommendations.value = data.recommendations || []

    // 通过 reason 判断是否是 mock 兜底（后端 mock 文案带"降级 Mock"）
    isFallback.value = recommendations.value.some(r => r.reason?.includes('降级 Mock'))

    if (recommendations.value.length === 0) {
      errorMsg.value = '没有匹配的菜谱，试试换种食材'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || 'AI 服务暂时不可用'
    recommendations.value = []
  } finally {
    stopProgress()
    setTimeout(() => { loading.value = false }, 200)
  }
}

onLoad(() => {
  // 优先用缓存（拍照页直接传过来）
  const cached = uni.getStorageSync('ai_recommendations')
  if (cached && cached.length) {
    recommendations.value = cached
    isFallback.value = recommendations.value.some(r => r.reason?.includes('降级 Mock'))
  } else {
    // 缓存空就重新调
    loadRecommend()
  }
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}
function goBack() { uni.navigateBack() }

async function addToToday(id: string) {
  try {
    await checkinApi.checkin(id)
    feedback.success('已加入今日')
  } catch (e) { console.error(e) }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; background: #F7F7F7; min-height: 100vh; }

.loading-area { padding: 200rpx 32rpx; text-align: center;
  .loading-icon { font-size: 96rpx; animation: bob 1.5s ease-in-out infinite; }
  .loading-title { font-size: 32rpx; font-weight: 600; margin-top: 24rpx; }
  .loading-sub { font-size: 26rpx; color: #FF6644; margin-top: 12rpx; min-height: 36rpx; }
  .progress-bar { width: 100%; height: 12rpx; background: #f5f5f5; border-radius: 6rpx; overflow: hidden; margin: 32rpx 0 16rpx;
    .progress-inner { height: 100%; background: linear-gradient(90deg, #FF8866, #FFB199); transition: width .4s; }
  }
  .loading-tip { font-size: 22rpx; color: #ccc; }
}
@keyframes bob { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-12rpx)} }

.empty { padding: 160rpx 32rpx; text-align: center;
  .empty-emoji { font-size: 120rpx; opacity: 0.5; }
  .empty-title { font-size: 32rpx; font-weight: 600; margin-top: 24rpx; }
  .empty-sub { font-size: 26rpx; color: #999; margin-top: 12rpx; }
  .btn-retry { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; margin: 48rpx 0 24rpx; }
  .alt { color: #999; font-size: 24rpx; padding: 16rpx; }
}

.hint { text-align: center; color: #FF8866; margin-bottom: 24rpx; font-size: 28rpx; }
.fallback-tip {
  background: #FFF4E5; color: #B57100;
  padding: 16rpx 24rpx; border-radius: 12rpx;
  font-size: 24rpx; margin-bottom: 16rpx;
}

.recipe-card { display: flex; background: #fff; border-radius: 24rpx; padding: 32rpx 24rpx; margin-bottom: 24rpx;
  .rank { font-size: 56rpx; margin-right: 24rpx; }
  .content { flex: 1; }
  .title { font-size: 30rpx; font-weight: 600;
    .score { font-size: 22rpx; color: #FF8866; background: #FFE5DC; padding: 4rpx 12rpx; border-radius: 8rpx; margin-left: 12rpx; }
  }
  .reason { font-size: 24rpx; color: #666; margin-top: 12rpx; line-height: 1.5; }
  .meta { display: flex; gap: 16rpx; font-size: 22rpx; color: #999; margin-top: 12rpx; }
  .missing { font-size: 24rpx; color: #FF6644; margin-top: 12rpx; }
  .no-missing { font-size: 24rpx; color: #4CAF50; margin-top: 12rpx; }
  .actions { margin-top: 16rpx;
    .btn-link { color: #FF8866; font-size: 26rpx; margin-right: 32rpx; }
  }
}
.disclaimer { text-align: center; color: #999; font-size: 22rpx; padding: 32rpx 0; }
</style>
