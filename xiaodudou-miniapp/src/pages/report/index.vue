<template>
  <view class="page">
    <view v-if="!userStore.isLoggedIn" class="empty">
      <view class="empty-emoji">📊</view>
      <view class="empty-title">登录后查看营养报告</view>
      <button class="btn-primary" @tap="goLogin">立即登录</button>
    </view>

    <template v-else>
      <!-- 顶部：阶段 + Tab -->
      <view class="header">
        <view class="stage">{{ report?.stageLabel || '阶段未设' }} · 营养报告</view>
        <view class="tabs">
          <view v-for="opt in tabs" :key="opt.value" class="tab" :class="{ active: days === opt.value }" @tap="switchTab(opt.value)">{{ opt.label }}</view>
        </view>
      </view>

      <view v-if="loading" class="loading">加载中...</view>

      <template v-else-if="report">
        <!-- 摘要卡 -->
        <view class="summary-card">
          <view class="summary-item">
            <view class="summary-num">{{ report.checkinDays }}</view>
            <view class="summary-label">打卡天数</view>
          </view>
          <view class="summary-divider" />
          <view class="summary-item">
            <view class="summary-num">{{ report.checkinCount }}</view>
            <view class="summary-label">打卡次数</view>
          </view>
          <view class="summary-divider" />
          <view class="summary-item">
            <view class="summary-num">{{ overallScore }}<text class="summary-unit">%</text></view>
            <view class="summary-label">综合达标</view>
          </view>
        </view>

        <!-- 雷达图（SVG） -->
        <view class="radar-card">
          <view class="card-title">营养摄入雷达</view>
          <view v-if="report.checkinCount === 0" class="radar-empty">
            <view class="empty-emoji">🍽️</view>
            <view>暂无数据，去打卡几次试试</view>
            <button class="btn-ghost" @tap="goRecipes">去选菜</button>
          </view>
          <view v-else class="radar-wrap">
            <!-- #ifdef H5 -->
            <view class="radar-svg" v-html="radarSvg" />
            <!-- #endif -->
            <!-- #ifndef H5 -->
            <view class="radar-fallback">📱 小程序端雷达图开发中，请看下方明细</view>
            <!-- #endif -->
          </view>
        </view>

        <!-- 明细卡 -->
        <view class="detail-card">
          <view class="card-title">详细数据（日均）</view>
          <view v-for="it in report.items" :key="it.key" class="detail-item">
            <view class="detail-row">
              <text class="detail-name">{{ it.name }}</text>
              <text class="detail-val">{{ it.avgActual }} / {{ it.target }} {{ it.unit }}</text>
              <text class="detail-pct" :class="pctClass(it.avgPercent)">{{ it.avgPercent }}%</text>
            </view>
            <view class="detail-bar">
              <view class="detail-bar-fill" :style="{ width: Math.min(100, it.avgPercent) + '%', background: pctColor(it.avgPercent) }" />
            </view>
          </view>
        </view>

        <!-- 热门菜谱 -->
        <view v-if="report.topRecipes && report.topRecipes.length > 0" class="recipes-card">
          <view class="card-title">期间常打卡</view>
          <view v-for="(r, idx) in report.topRecipes" :key="r.recipeId" class="recipe-row" @tap="goDetail(r.recipeId)">
            <text class="rank">{{ idx + 1 }}</text>
            <text class="recipe-title">{{ r.title }}</text>
            <text class="recipe-count">{{ r.count }} 次 ›</text>
          </view>
        </view>

        <!-- 建议 -->
        <view class="tips-card">
          <view class="card-title">💡 改善建议</view>
          <view v-for="(tip, i) in tips" :key="i" class="tip">· {{ tip }}</view>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { nutritionApi, type NutritionReport } from '../../api/nutrition'

const userStore = useUserStore()
const days = ref<1 | 7 | 30>(7)
const loading = ref(false)
const report = ref<NutritionReport | null>(null)

const tabs = [
  { label: '今日', value: 1 as const },
  { label: '7日', value: 7 as const },
  { label: '30日', value: 30 as const }
]

onShow(() => {
  if (userStore.isLoggedIn) fetchReport()
})

async function fetchReport() {
  loading.value = true
  try {
    report.value = await nutritionApi.report(days.value)
  } finally {
    loading.value = false
  }
}

function switchTab(v: 1 | 7 | 30) {
  if (days.value === v) return
  days.value = v
  fetchReport()
}

// 综合达标 = 6 维度 avgPercent 算数平均（截至 100%）
const overallScore = computed(() => {
  if (!report.value?.items || report.value.items.length === 0) return 0
  const sum = report.value.items.reduce((s, it) => s + Math.min(100, it.avgPercent), 0)
  return Math.round(sum / report.value.items.length)
})

function pctClass(p: number) {
  if (p >= 100) return 'good'
  if (p >= 60) return 'mid'
  return 'low'
}

function pctColor(p: number) {
  if (p >= 100) return 'linear-gradient(90deg, #66BB6A, #4CAF50)'
  if (p >= 60) return 'linear-gradient(90deg, #FFB199, #FF8866)'
  return 'linear-gradient(90deg, #FFD2C2, #FFB199)'
}

// 智能建议（最低 2 个维度）
const tips = computed(() => {
  if (!report.value?.items || report.value.checkinCount === 0) {
    return ['暂无数据，建议每天打卡 2-3 餐以生成更准确的报告']
  }
  const low = [...report.value.items].sort((a, b) => a.avgPercent - b.avgPercent).slice(0, 2)
  const lines: string[] = []
  for (const it of low) {
    if (it.avgPercent >= 80) continue
    lines.push(`${it.name}摄入偏低（${it.avgPercent}%），建议增加${suggestFood(it.key)}`)
  }
  if (lines.length === 0) lines.push('各项营养均达到推荐水平，继续保持 👍')
  return lines
})

function suggestFood(key: string) {
  const map: Record<string, string> = {
    calories: '主食与坚果摄入',
    protein: '鱼肉蛋奶豆制品',
    calcium: '牛奶、豆腐、深绿叶菜',
    iron: '红肉、动物肝脏、菠菜',
    vitA: '胡萝卜、南瓜、动物肝脏',
    vitC: '彩椒、柑橘、猕猴桃'
  }
  return map[key] || '均衡饮食'
}

// SVG 雷达图（仅 H5 渲染，小程序端 M2 用 uni-charts 替换）
const radarSvg = computed(() => {
  if (!report.value?.items || report.value.items.length === 0) return ''
  const items = report.value.items
  const cx = 200, cy = 200, R = 130
  const n = items.length

  // 100% 处的标尺圆，及 50/100/150 三圈参考网格
  // 数据点：percent → 半径 = (percent/100) * R，截至 1.5R（150%）
  const angleAt = (i: number) => (-90 + (360 / n) * i) * Math.PI / 180
  const pointAt = (i: number, r: number) => {
    const a = angleAt(i)
    return [cx + Math.cos(a) * r, cy + Math.sin(a) * r]
  }

  // 参考网格 3 层
  const grids = [0.5, 1.0, 1.5].map((scale) => {
    const pts = items.map((_, i) => pointAt(i, R * scale).join(',')).join(' ')
    return `<polygon points="${pts}" fill="none" stroke="#E5E5E5" stroke-width="1" />`
  }).join('')

  // 轴线
  const axes = items.map((_, i) => {
    const [x, y] = pointAt(i, R * 1.5)
    return `<line x1="${cx}" y1="${cy}" x2="${x}" y2="${y}" stroke="#E5E5E5" stroke-width="1" />`
  }).join('')

  // 数据多边形
  const dataPts = items.map((it, i) => {
    const ratio = Math.min(1.5, it.avgPercent / 100)
    return pointAt(i, R * ratio).join(',')
  }).join(' ')

  // 数据点小圆
  const dots = items.map((it, i) => {
    const ratio = Math.min(1.5, it.avgPercent / 100)
    const [x, y] = pointAt(i, R * ratio)
    return `<circle cx="${x}" cy="${y}" r="4" fill="#FF8866" />`
  }).join('')

  // 维度标签
  const labels = items.map((it, i) => {
    const [x, y] = pointAt(i, R * 1.5 + 18)
    const anchor = Math.abs(x - cx) < 10 ? 'middle' : (x < cx ? 'end' : 'start')
    return `<text x="${x}" y="${y}" text-anchor="${anchor}" dominant-baseline="middle" font-size="14" fill="#666">${it.name}</text>`
  }).join('')

  return `<svg viewBox="0 0 400 400" xmlns="http://www.w3.org/2000/svg" style="width:100%;height:100%;">
    ${grids}
    ${axes}
    <polygon points="${dataPts}" fill="rgba(255,136,102,0.25)" stroke="#FF8866" stroke-width="2" />
    ${dots}
    ${labels}
    <text x="${cx}" y="${cy - R * 1.5 - 4}" text-anchor="middle" font-size="11" fill="#bbb">150%</text>
    <text x="${cx}" y="${cy - R - 4}" text-anchor="middle" font-size="11" fill="#bbb">100%</text>
    <text x="${cx}" y="${cy - R * 0.5 - 4}" text-anchor="middle" font-size="11" fill="#bbb">50%</text>
  </svg>`
})

function goLogin() {
  uni.navigateTo({ url: '/pages/auth/wx-login' })
}
function goRecipes() {
  uni.switchTab({ url: '/pages/recipe/list' })
}
function goDetail(id: number | string) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; padding-bottom: 80rpx; }

.empty {
  text-align: center; padding-top: 240rpx;
  .empty-emoji { font-size: 120rpx; }
  .empty-title { font-size: 28rpx; color: #999; margin: 32rpx 0; }
  .btn-primary { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; width: 60%; }
}

.header {
  background: #fff; border-radius: 20rpx;
  padding: 32rpx 24rpx; margin-bottom: 24rpx;
  .stage { font-size: 28rpx; font-weight: 600; color: #333; }
  .tabs {
    display: flex; margin-top: 24rpx;
    background: #F7F7F7; border-radius: 32rpx; padding: 4rpx;
    .tab {
      flex: 1; text-align: center; padding: 16rpx 0;
      font-size: 26rpx; color: #999; border-radius: 28rpx;
      &.active { background: #fff; color: #FF8866; font-weight: 600; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05); }
    }
  }
}

.loading { text-align: center; padding: 80rpx; color: #999; font-size: 26rpx; }

.summary-card {
  background: linear-gradient(135deg, #FFE5DC, #FFD2C2);
  border-radius: 20rpx; padding: 32rpx 24rpx;
  margin-bottom: 24rpx; display: flex; align-items: center;
  .summary-item { flex: 1; text-align: center;
    .summary-num { font-size: 44rpx; font-weight: 700; color: #333;
      .summary-unit { font-size: 24rpx; font-weight: 400; color: #666; margin-left: 4rpx; }
    }
    .summary-label { font-size: 22rpx; color: #666; margin-top: 4rpx; }
  }
  .summary-divider { width: 1rpx; height: 60rpx; background: rgba(0,0,0,0.08); }
}

.radar-card, .detail-card, .recipes-card, .tips-card {
  background: #fff; border-radius: 20rpx;
  padding: 32rpx 24rpx; margin-bottom: 24rpx;
  .card-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 24rpx; }
}

.radar-wrap { padding: 16rpx 0; }
.radar-svg { width: 100%; aspect-ratio: 1 / 1; }
.radar-fallback { text-align: center; color: #999; font-size: 24rpx; padding: 40rpx; }
.radar-empty { text-align: center; padding: 40rpx;
  .empty-emoji { font-size: 80rpx; }
  .btn-ghost { background: #fff; color: #FF8866; border: 1rpx solid #FFE5DC;
    height: 72rpx; line-height: 72rpx; border-radius: 36rpx; font-size: 26rpx;
    margin-top: 24rpx; width: 60%; }
}

.detail-item {
  padding: 18rpx 0; border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  .detail-row {
    display: flex; align-items: center;
    .detail-name { width: 120rpx; font-size: 26rpx; color: #333; }
    .detail-val { flex: 1; font-size: 24rpx; color: #999; }
    .detail-pct {
      font-size: 26rpx; font-weight: 600;
      &.good { color: #4CAF50; }
      &.mid { color: #FF8866; }
      &.low { color: #FFB199; }
    }
  }
  .detail-bar {
    background: #f5f5f5; border-radius: 6rpx; overflow: hidden;
    height: 12rpx; margin-top: 12rpx;
    .detail-bar-fill { height: 100%; transition: width .3s; }
  }
}

.recipe-row {
  display: flex; align-items: center; padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  .rank { width: 40rpx; height: 40rpx; background: #FFE5DC; color: #FF8866;
    border-radius: 50%; text-align: center; line-height: 40rpx;
    font-size: 24rpx; font-weight: 600; margin-right: 16rpx; }
  .recipe-title { flex: 1; font-size: 28rpx; color: #333; }
  .recipe-count { font-size: 24rpx; color: #999; }
}

.tips-card {
  .tip { font-size: 26rpx; color: #666; line-height: 1.7; padding: 6rpx 0; }
}
</style>
