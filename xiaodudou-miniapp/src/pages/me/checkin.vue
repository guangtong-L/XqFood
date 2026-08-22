<template>
  <view class="page">
    <!-- 月度统计 -->
    <view class="stat-card">
      <view class="month-row">
        <text class="nav" @tap="changeMonth(-1)">‹</text>
        <text class="month">{{ currentMonth }}</text>
        <text class="nav" @tap="changeMonth(1)">›</text>
      </view>
      <view v-if="data" class="stats">
        <view class="stat">
          <view class="num">{{ data?.checkinDays || 0 }}</view>
          <view class="label">打卡天数</view>
        </view>
        <view class="stat">
          <view class="num">{{ data?.totalCheckins || 0 }}</view>
          <view class="label">总打卡数</view>
        </view>
        <view class="stat">
          <view class="num">{{ streakDays }}</view>
          <view class="label">连续天数</view>
        </view>
      </view>
      <view v-else class="stats-unavailable">统计暂不可用</view>
    </view>

    <view v-if="loading" class="state-box">正在加载记录...</view>
    <view v-else-if="errorMsg" class="state-box error">{{ errorMsg }}<button @tap="loadAll">重试</button></view>

    <!-- 日历 -->
    <view v-else class="calendar">
      <view class="weekdays">
        <text v-for="d in ['一','二','三','四','五','六','日']" :key="d">{{ d }}</text>
      </view>
      <view class="days">
        <view v-for="(cell, idx) in cells" :key="idx"
              class="day-cell"
              :class="{
                empty: !cell.day,
                today: cell.isToday,
                checked: cell.count > 0
              }">
          <text v-if="cell.day" class="day-num">{{ cell.day }}</text>
          <view v-if="cell.count > 0" class="dot">{{ cell.count }}</view>
        </view>
      </view>
    </view>

    <!-- 今日打卡列表 -->
    <view v-if="!loading && !errorMsg" class="today-section">
      <view class="section-title">📅 今日已记录（{{ todayList.length }} 项）</view>
      <view v-if="todayList.length === 0" class="today-empty">
        今天还没有确认完成/已食用的菜谱
      </view>
      <view v-else class="today-list">
        <view v-for="t in todayList" :key="t.actionId" class="today-item" @tap="goDetail(t.recipeId)">
          <image v-if="t.coverUrl && !failedImages.has(String(t.actionId))" :src="t.coverUrl" class="t-img" mode="aspectFill" @error="markImageFailed(t.actionId)" />
          <view v-else class="t-img image-fallback">🍲</view>
          <view class="t-info">
            <view class="t-title">{{ t.title }}</view>
            <view class="t-meta">
              <text>{{ mealLabel(t.mealType) }}</text>
              <text v-if="t.servings">{{ t.servings }} 份</text>
              <text>{{ formatTime(t.checkedAt) }}</text>
            </view>
          </view>
          <text class="remove" :class="{ disabled: deletingIds.has(String(t.actionId)) }" @tap.stop="removeCheckin(t.actionId)">{{ deletingIds.has(String(t.actionId)) ? '处理中' : '删除' }}</text>
        </view>
      </view>
    </view>

    <view class="estimate-note">营养汇总仅依据您主动记录的菜谱和份数进行估算，不代表全天摄入。</view>
    <button class="btn-add" @tap="goRecipes">+ 选择菜谱并确认记录</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { checkinApi, type CalendarData, type CheckinItem } from '../../api/checkin'
import { feedback } from '../../utils/feedback'

const data = ref<CalendarData | null>(null)
const todayList = ref<CheckinItem[]>([])
const loading = ref(false)
const errorMsg = ref('')
const deletingIds = ref(new Set<string>())
const failedImages = ref(new Set<string>())

// 当前选中月份（YYYY-MM）
const now = new Date()
const curYear = ref(now.getFullYear())
const curMonth = ref(now.getMonth() + 1)

const currentMonth = computed(() => `${curYear.value} 年 ${curMonth.value} 月`)
const monthStr = computed(() => `${curYear.value}-${String(curMonth.value).padStart(2, '0')}`)

// 日历单元格
const cells = computed(() => {
  const result: Array<{ day: number | null; count: number; isToday: boolean }> = []
  const firstDay = new Date(curYear.value, curMonth.value - 1, 1)
  const daysInMonth = new Date(curYear.value, curMonth.value, 0).getDate()
  // 周一为首：getDay() 返回 0(日)-6(六)，转成 mon=0
  const startOffset = (firstDay.getDay() + 6) % 7

  for (let i = 0; i < startOffset; i++) result.push({ day: null, count: 0, isToday: false })

  const today = new Date()
  for (let d = 1; d <= daysInMonth; d++) {
    const key = `${monthStr.value}-${String(d).padStart(2, '0')}`
    const count = data.value?.dayCount?.[key] || 0
    const isToday = today.getFullYear() === curYear.value
                  && today.getMonth() + 1 === curMonth.value
                  && today.getDate() === d
    result.push({ day: d, count, isToday })
  }
  return result
})

// 连续打卡天数（简单计算：从今天往前数有几天连续 count>0）
const streakDays = computed(() => {
  if (!data.value?.dayCount) return 0
  let streak = 0
  const today = new Date()
  for (let i = 0; i < 31; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    if ((data.value.dayCount[key] || 0) > 0) streak++
    else if (i > 0) break  // 今天没打卡也算 streak 中断，但要从昨天开始数
  }
  return streak
})

async function changeMonth(delta: number) {
  if (loading.value) return
  const previousYear = curYear.value
  const previousMonth = curMonth.value
  let y = curYear.value, m = curMonth.value + delta
  if (m < 1) { m = 12; y-- }
  else if (m > 12) { m = 1; y++ }
  curYear.value = y
  curMonth.value = m
  const loaded = await loadAll('月份加载失败，已恢复原月份，当前统计仍为上次结果')
  if (!loaded) {
    curYear.value = previousYear
    curMonth.value = previousMonth
  }
}

async function loadCalendar(month: string) {
  return checkinApi.calendar(month)
}

async function loadToday() {
  return await checkinApi.today() || []
}

async function loadAll(failureMessage = '记录刷新失败，当前为上次结果') {
  if (loading.value) return false
  loading.value = true
  errorMsg.value = ''
  const requestedMonth = monthStr.value
  try {
    const [calendar, today] = await Promise.all([loadCalendar(requestedMonth), loadToday()])
    if (requestedMonth !== monthStr.value) return false
    data.value = calendar
    todayList.value = today
    return true
  } catch {
    errorMsg.value = failureMessage
    return false
  } finally {
    loading.value = false
  }
}

const mealLabel = (meal?: string) => ({ breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' } as Record<string, string>)[meal || ''] || '历史记录'

async function removeCheckin(actionId: string) {
  const key = String(actionId)
  if (deletingIds.value.has(key)) return
  const ok = await feedback.confirm('确认删除这条本人记录？')
  if (!ok) return
  deletingIds.value = new Set(deletingIds.value).add(key)
  try {
    await checkinApi.remove(actionId)
    const refreshed = await loadAll('删除成功，但列表刷新失败，当前为上次结果')
    feedback.success(refreshed ? '已删除' : '已删除，请重新刷新')
  } catch (error) {
    feedback.error(error instanceof Error ? error.message : '删除失败，请重试')
  } finally {
    const next = new Set(deletingIds.value); next.delete(key); deletingIds.value = next
  }
}

function markImageFailed(id: string | number) { failedImages.value = new Set(failedImages.value).add(String(id)) }

function formatTime(s: string) {
  // s 形如 "2026-05-23T12:34:56"
  const m = s.match(/T(\d{2}:\d{2})/)
  return m ? m[1] : ''
}

function goDetail(id: string) { uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` }) }
function goRecipes() { uni.switchTab({ url: '/pages/recipe/list' }) }

onShow(loadAll)
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; padding-bottom: 200rpx; }
.state-box { padding: 100rpx 24rpx; text-align: center; color: #777; background: #fff; border-radius: 20rpx;
  button { margin-top: 24rpx; min-height: 80rpx; width: 220rpx; border-radius: 40rpx; background: #FFF0EB; color: #D94F2B; }
}

.stat-card { background: linear-gradient(135deg, #FF8866, #FFB199); border-radius: 24rpx; padding: 32rpx; color: #fff; margin-bottom: 24rpx;
  .month-row { display: flex; justify-content: center; align-items: center; gap: 48rpx; margin-bottom: 32rpx;
    .nav { font-size: 48rpx; padding: 8rpx 24rpx; }
    .month { font-size: 32rpx; font-weight: 600; }
  }
  .stats { display: flex; justify-content: space-around;
    .stat { text-align: center;
      .num { font-size: 48rpx; font-weight: bold; }
      .label { font-size: 22rpx; opacity: 0.9; margin-top: 8rpx; }
    }
  }
  .stats-unavailable { padding: 24rpx 0; text-align: center; font-size: 25rpx; opacity: .9; }
}

.calendar { background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 24rpx;
  .weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0;
    text { font-size: 24rpx; color: #999; }
  }
  .days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8rpx; padding-top: 16rpx;
    .day-cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; position: relative;
      &.empty { opacity: 0; }
      .day-num { font-size: 26rpx; color: #333; }
      &.today .day-num { color: #FF6644; font-weight: bold; }
      &.checked {
        .day-num { color: #fff; position: relative; z-index: 2; }
        &::before {
          content: ''; position: absolute; inset: 8rpx;
          background: linear-gradient(135deg, #FF8866, #FFB199);
          border-radius: 50%; z-index: 1;
        }
      }
      .dot { position: absolute; top: 2rpx; right: 4rpx; background: #fff; color: #FF6644;
        font-size: 16rpx; padding: 1rpx 6rpx; border-radius: 8rpx; z-index: 3; min-width: 16rpx; text-align: center; }
    }
  }
}

.today-section { background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 24rpx;
  .section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; }
  .today-empty { color: #999; font-size: 26rpx; padding: 32rpx; text-align: center; }
  .today-list .today-item { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .t-img { width: 120rpx; height: 120rpx; border-radius: 12rpx; background: #f0f0f0; }
    .image-fallback { display: flex; align-items: center; justify-content: center; font-size: 42rpx; color: #aaa; flex-shrink: 0; }
    .t-info { flex: 1; padding-left: 16rpx;
      .t-title { font-size: 28rpx; font-weight: 500; }
      .t-meta { display: flex; gap: 16rpx; font-size: 22rpx; color: #999; margin-top: 8rpx; }
    }
    .remove { align-self: center; color: #777; font-size: 22rpx; padding: 20rpx; min-width: 64rpx; text-align: center;
      &.disabled { opacity: .5; }
    }
  }
}
.estimate-note { margin-bottom: 130rpx; padding: 20rpx 24rpx; color: #777; background: #FFF8F5; border-radius: 14rpx; font-size: 22rpx; line-height: 1.6; }

.btn-add {
  position: fixed; left: 32rpx; right: 32rpx; bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #FF8866; color: #fff;
  height: 88rpx; line-height: 88rpx; border-radius: 44rpx;
  font-size: 30rpx; box-shadow: 0 8rpx 24rpx rgba(255,136,102,0.4);
}

</style>
