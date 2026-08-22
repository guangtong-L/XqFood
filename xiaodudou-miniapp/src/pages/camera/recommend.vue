<template>
  <view class="page">
    <view v-if="!AI_FEATURE_ENABLED" class="feature-closed">
      <view class="empty-emoji">🍲</view>
      <view class="empty-title">AI 推荐暂未开放</view>
      <view class="empty-sub">内容安全审核能力仍在接入验收中，请先浏览现有菜谱。</view>
      <button class="btn-retry" @tap="goRecipes">去浏览菜谱</button>
    </view>
    <template v-else>
    <!-- Loading -->
    <view v-if="loading" class="loading-area">
      <view class="loading-icon">🤖</view>
      <view class="loading-title">AI 正在为你挑菜</view>
      <view class="loading-sub">{{ loadingText }}</view>
      <view class="progress-bar">
        <view class="progress-inner" :style="{ width: progress + '%' }"></view>
      </view>
      <view class="loading-tip">处理时间受网络与外部服务状态影响</view>
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
      <view class="ai-label">{{ aiLabel }}</view>
      <view class="hint">基于所填食材与上架菜谱的 AI 参考排序</view>

      <view v-if="isFallback" class="fallback-tip">
        ⚠️ 当前为开发模式模拟结果，不代表真实 AI 推荐
      </view>

      <view class="recipe-card" v-for="(r, idx) in recommendations" :key="r.recipe.id" @tap="goDetail(r.recipe.id)">
        <view class="rank">{{ ['🥇','🥈','🥉'][idx] || '🏅' }}</view>
        <view class="content">
          <view class="title">
            {{ r.recipe.title }}
            <text class="score">食材匹配参考 {{ r.matchScore }}</text>
          </view>
          <view class="reason">{{ r.reason }}</view>
          <view class="meta">
            <text v-if="r.recipe.cookMinutes">🕐 {{ r.recipe.cookMinutes }} 分钟</text>
          </view>
          <view v-if="r.missingIngredients && r.missingIngredients.length" class="missing">
            缺料：{{ r.missingIngredients.map(m => m.name).join('、') }}
          </view>
          <view v-else class="no-missing">未列出缺料，请按做法再次核对</view>
          <view class="actions">
            <text class="btn-link" @tap.stop="goDetail(r.recipe.id)">查看做法 →</text>
          </view>
        </view>
      </view>

      <view class="allergy-notice">{{ allergyNotice }}</view>
      <view class="disclaimer">{{ disclaimer }}</view>
    </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { aiApi, type AiRecommendation } from '../../api/ai'
import { AI_FEATURE_ENABLED } from '../../config/features'

const recommendations = ref<AiRecommendation[]>([])
const loading = ref(false)
const loadingText = ref('调用 AI 中...')
const progress = ref(0)
const errorMsg = ref('')
const isFallback = ref(false)
const aiLabel = ref('AI辅助生成')
const disclaimer = ref('AI 生成内容仅供参考，不构成医疗或营养建议，请人工核对食材与做法。')
const allergyNotice = ref('仅基于现有标签降低冲突风险，仍需人工核对。')

let progressTimer: ReturnType<typeof setInterval> | null = null

function startProgress() {
  progress.value = 0
  loadingText.value = '准备食材清单...'
  progressTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value = Math.min(90, progress.value + (progress.value < 60 ? 6 : 2))
      if (progress.value < 25) loadingText.value = '准备食材清单...'
      else if (progress.value < 55) loadingText.value = '调用 AI 服务...'
      else if (progress.value < 80) loadingText.value = '核对上架候选菜谱...'
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
    if (!cached?.ingredients?.length) throw new Error('请返回上一步，至少添加一种食材')
    const data = await aiApi.recommend(cached)
    recommendations.value = data.recommendations || []

    isFallback.value = data.fallback
    aiLabel.value = data.aiLabel || 'AI辅助生成'
    disclaimer.value = data.disclaimer || disclaimer.value
    allergyNotice.value = data.allergyNotice || allergyNotice.value

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
  if (!AI_FEATURE_ENABLED) return
  loadRecommend()
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}
function goBack() { uni.navigateBack() }
function goRecipes() { uni.switchTab({ url: '/pages/recipe/list' }) }

</script>

<style lang="scss" scoped>
.page { padding: 32rpx; background: #F7F7F7; min-height: 100vh; }
.feature-closed { padding: 160rpx 32rpx; text-align: center;
  .empty-emoji { font-size: 112rpx; }
  .empty-title { margin-top: 24rpx; font-size: 32rpx; font-weight: 600; }
  .empty-sub { margin-top: 16rpx; color: #777; font-size: 26rpx; line-height: 1.6; }
  .btn-retry { margin-top: 42rpx; min-height: 88rpx; border-radius: 44rpx; background: #FF8866; color: #fff; }
}

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
.ai-label { width: fit-content; margin: 0 auto 16rpx; padding: 8rpx 16rpx; border-radius: 8rpx; background: #FFF0EB; color: #D94F2B; font-size: 22rpx; }
.fallback-tip {
  background: #FFF4E5; color: #B57100;
  padding: 16rpx 24rpx; border-radius: 12rpx;
  font-size: 24rpx; margin-bottom: 16rpx;
}
.allergy-notice { margin-top: 24rpx; padding: 20rpx; border-radius: 12rpx; background: #FFF7E8; color: #7A5600; font-size: 23rpx; line-height: 1.6; }

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
