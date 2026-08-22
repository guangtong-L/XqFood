<template>
  <view v-if="pageState === 'loading'" class="loading">正在加载菜谱...</view>
  <view v-else-if="pageState === 'error'" class="load-error">{{ loadError }}<button @tap="loadDetail">重试</button></view>
  <view class="page" v-else-if="data">
    <view class="cover-wrap">
      <image v-if="data.recipe.coverUrl && !coverFailed" class="cover" :src="data.recipe.coverUrl" mode="aspectFill" @error="coverFailed = true" />
      <view v-else class="cover cover-fallback">🍲<text>图片暂不可用</text></view>
      <view class="fav-btn" :class="{ active: favorited, disabled: favoriteLoading }" @tap="toggleFavorite">
        {{ favoriteLoading ? '…' : (favorited ? '❤️' : '🤍') }}
      </view>
    </view>

    <view class="header">
      <view class="title">{{ data.recipe.title }}</view>
      <view class="meta">
        <text v-if="data.recipe.cookMinutes">🕐 {{ data.recipe.cookMinutes }} 分钟</text>
        <text>难度 {{ '★'.repeat(data.recipe.difficulty || 1) }}</text>
      </view>
      <view class="tags">
        <text v-for="t in (data.recipe.stageTags || [])" :key="t" class="tag">{{ stageLabel(t) }}</text>
      </view>
    </view>

    <view class="card" v-if="data.recipe.nutrition">
      <view class="card-title">━ 营养（每份）━</view>
      <view class="nutri-row">
        <view class="nutri" v-for="n in nutritionItems" :key="n.k">
          <view class="val">{{ n.v }}</view>
          <view class="lbl">{{ n.k }}</view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">━ 食材 ━</view>
      <view class="ing-list">
        <view v-for="ri in data.recipeIngredients" :key="ri.ingredientId" class="ing-row">
          <view>
            <text>✅ {{ findIngName(ri.ingredientId) }}</text>
            <text v-for="tag in findAllergens(ri.ingredientId)" :key="tag" class="allergen">含 {{ allergenLabel(tag) }}</text>
          </view>
          <text class="qty">{{ ri.quantity }}</text>
        </view>
      </view>
      <view class="allergen-hint">过敏标签仅作信息提示，请结合个人情况和包装/食材来源再次核对。</view>
    </view>

    <view class="card" v-if="data.recipe.steps && data.recipe.steps.length">
      <view class="card-title">━ 步骤 ━</view>
      <view v-for="(s, idx) in data.recipe.steps" :key="idx" class="step-row">
        <view class="step-num">{{ idx + 1 }}</view>
        <view class="step-body">
          <view class="step-desc">{{ s.desc }}</view>
          <view v-if="s.timer" class="step-timer" @tap="startTimer(s.timer)">⏱️ {{ formatTimer(s.timer) }}</view>
        </view>
      </view>
    </view>

    <view class="disclaimer">食谱与标签仅作信息参考，不构成医疗或营养建议，请结合个人情况核对。</view>

    <view class="footer-bar">
      <view class="footer-fav" :class="{ disabled: favoriteLoading }" @tap="toggleFavorite">
        <text>{{ favoriteLoading ? '…' : (favorited ? '❤️' : '🤍') }}</text>
        <text class="footer-fav-text">{{ favoriteLoading ? '处理中' : (favorited ? '已收藏' : '收藏') }}</text>
      </view>
      <button class="footer-btn" @tap="doCheckin" :disabled="checkinLoading">
        {{ checkinLoading ? '记录中...' : '✓ 我已完成/已食用' }}
      </button>
    </view>

    <view v-if="checkinDialog" class="dialog-mask" @tap.self="checkinDialog = false">
      <view class="dialog">
        <view class="dialog-title">确认记录已食用</view>
        <view class="dialog-hint">请选择真实餐次和份数。日期仅支持今天或过去 7 天补录。</view>
        <view class="dialog-label">餐次</view>
        <view class="meal-options">
          <view v-for="item in mealOptions" :key="item.value" class="meal-option"
                :class="{ active: checkinForm.mealType === item.value }" @tap="checkinForm.mealType = item.value">
            {{ item.label }}
          </view>
        </view>
        <view class="dialog-label">份数（0.25–10）</view>
        <input class="dialog-input" type="digit" :value="checkinForm.servings" @input="onServingsInput" />
        <view class="dialog-label">记录日期</view>
        <picker mode="date" :start="earliestDate" :end="today" :value="checkinForm.actionDate" @change="onDateChange">
          <view class="dialog-input">{{ checkinForm.actionDate }}</view>
        </picker>
        <view class="dialog-actions">
          <button class="dialog-cancel" @tap="checkinDialog = false">取消</button>
          <button class="dialog-confirm" :disabled="checkinLoading" @tap="confirmCheckin">确认记录</button>
        </view>
      </view>
    </view>

    <view v-if="countdown" class="timer-mask" @tap.self="closeTimer">
      <view class="timer-dialog" role="timer" aria-live="polite">
        <view class="timer-title">步骤倒计时</view>
        <view class="timer-value" :class="{ finished: countdown.finished }">
          {{ countdown.finished ? '计时结束' : formatCountdown(countdown.remainingSeconds) }}
        </view>
        <view class="timer-actions">
          <button :disabled="countdown.finished" @tap="toggleTimer">{{ countdown.running ? '暂停' : '继续' }}</button>
          <button @tap="resetTimer">重置</button>
          <button @tap="closeTimer">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import { recipeApi, type RecipeDetail } from '../../api/recipe'
import { favoriteApi } from '../../api/favorite'
import { checkinApi } from '../../api/checkin'
import { feedback } from '../../utils/feedback'
import { useUserStore } from '../../store/user'
import { buildCheckinInput, formatLocalDate, localDateDaysAgo, type MealType } from '../../utils/checkin'
import { createCountdown, formatCountdown, resetCountdown, tickCountdown, toggleCountdown, type CountdownState } from '../../utils/countdown'

const userStore = useUserStore()
const data = ref<RecipeDetail | null>(null)
const pageState = ref<'loading' | 'ready' | 'error'>('loading')
const loadError = ref('')
const recipeId = ref('')
const favorited = ref(false)
const favoriteLoading = ref(false)
const checkinLoading = ref(false)
const checkinDialog = ref(false)
const coverFailed = ref(false)
const today = formatLocalDate(new Date())
const earliestDate = localDateDaysAgo(7)
const mealOptions: Array<{ label: string; value: MealType }> = [
  { label: '早餐', value: 'breakfast' }, { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' }, { label: '加餐', value: 'snack' }
]
const checkinForm = ref<{ mealType?: MealType; servings: string; actionDate: string }>({ servings: '1', actionDate: today })
const countdown = ref<CountdownState | null>(null)
let countdownInterval: ReturnType<typeof setInterval> | null = null

async function toggleFavorite() {
  if (!userStore.isLoggedIn) { feedback.toast('请先登录'); return }
  if (!data.value || favoriteLoading.value) return
  const id = data.value.recipe.id
  favoriteLoading.value = true
  try {
    if (favorited.value) {
      await favoriteApi.remove(id)
      favorited.value = false
      feedback.toast('已取消收藏')
    } else {
      await favoriteApi.add(id)
      favorited.value = true
      feedback.success('已收藏')
    }
  } catch (e) {
    feedback.error(e instanceof Error ? e.message : '收藏操作失败，请重试')
  } finally {
    favoriteLoading.value = false
  }
}

async function doCheckin() {
  if (!userStore.isLoggedIn) { feedback.toast('请先登录'); return }
  if (!data.value) return
  checkinDialog.value = true
}

function onServingsInput(event: { detail: { value: string } }) { checkinForm.value.servings = event.detail.value }
function onDateChange(event: { detail: { value: string } }) { checkinForm.value.actionDate = event.detail.value }

async function confirmCheckin() {
  if (!data.value || checkinLoading.value) return
  if (!checkinForm.value.mealType) { feedback.toast('请选择餐次'); return }
  let input
  try {
    input = buildCheckinInput(data.value.recipe.id, checkinForm.value.mealType,
      Number(checkinForm.value.servings), checkinForm.value.actionDate)
  } catch (error) { feedback.toast(error instanceof Error ? error.message : '请检查记录信息'); return }
  checkinLoading.value = true
  try {
    const result = await checkinApi.checkin(input)
    checkinDialog.value = false
    feedback.success(result.alreadyExists ? '今日该餐已记录' : '已记录')
  } catch (e) {
    console.error(e)
  } finally {
    checkinLoading.value = false
  }
}

const STAGE_LABEL: Record<string, string> = {
  postpartum_early: '月子早期', postpartum_late: '月子后期',
  lactation: '哺乳期', weaning: '辅食', child: '儿童'
}
const stageLabel = (t: string) => STAGE_LABEL[t] || t

const nutritionItems = computed(() => {
  const n = (data.value?.recipe.nutrition || {}) as Record<string, number>
  const out: Array<{ k: string; v: string }> = []
  if (n.calories) out.push({ k: '热量', v: n.calories + ' kcal' })
  if (n.protein) out.push({ k: '蛋白', v: n.protein + ' g' })
  if (n.calcium) out.push({ k: '钙', v: n.calcium + ' mg' })
  if (n.iron) out.push({ k: '铁', v: n.iron + ' mg' })
  return out
})

function findIngName(id: string) {
  return data.value?.ingredients.find(i => String(i.id) === String(id))?.name || '未知'
}
function findAllergens(id: string) { return data.value?.ingredients.find(i => String(i.id) === String(id))?.allergenTags || [] }
const ALLERGEN_LABEL: Record<string, string> = { egg: '蛋类', milk: '乳制品', seafood: '海鲜' }
const allergenLabel = (tag: string) => ALLERGEN_LABEL[tag] || tag

function formatTimer(sec: number) {
  if (sec < 60) return sec + ' 秒'
  return Math.floor(sec / 60) + ' 分' + (sec % 60 ? (sec % 60) + ' 秒' : '')
}

function startTimer(sec: number) {
  if (countdown.value) return
  try {
    countdown.value = createCountdown(sec)
    syncCountdownInterval()
  } catch (error) {
    feedback.toast(error instanceof Error ? error.message : '计时秒数无效')
  }
}

function syncCountdownInterval() {
  if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
  if (!countdown.value?.running) return
  countdownInterval = setInterval(() => {
    if (!countdown.value) return clearCountdownInterval()
    const next = tickCountdown(countdown.value)
    const justFinished = !countdown.value.finished && next.finished
    countdown.value = next
    if (justFinished) {
      clearCountdownInterval()
      if (typeof uni.vibrateLong === 'function') uni.vibrateLong({ fail: () => undefined })
      feedback.success('步骤计时结束')
    }
  }, 1000)
}

function clearCountdownInterval() {
  if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
}

function toggleTimer() {
  if (!countdown.value) return
  countdown.value = toggleCountdown(countdown.value)
  syncCountdownInterval()
}

function resetTimer() {
  if (!countdown.value) return
  countdown.value = resetCountdown(countdown.value)
  syncCountdownInterval()
}

function closeTimer() {
  clearCountdownInterval()
  countdown.value = null
}

onHide(() => {
  if (countdown.value?.running) countdown.value = { ...countdown.value, running: false }
  clearCountdownInterval()
})
onUnload(closeTimer)

async function loadDetail() {
  if (!recipeId.value) return
  pageState.value = 'loading'
  loadError.value = ''
  coverFailed.value = false
  try {
    data.value = await recipeApi.detail(recipeId.value)
    pageState.value = 'ready'
    if (userStore.isLoggedIn) {
      try { favorited.value = await favoriteApi.check(recipeId.value) } catch { /* 不影响详情主链路 */ }
    }
  } catch (error) {
    data.value = null
    loadError.value = error instanceof Error ? error.message : '菜谱加载失败，请检查网络'
    pageState.value = 'error'
  }
}

onLoad((q: any) => {
  if (!q?.id) {
    loadError.value = '菜谱参数缺失'
    pageState.value = 'error'
    return
  }
  recipeId.value = String(q.id)
  loadDetail()
})
</script>

<style lang="scss" scoped>
.page { background: #F7F7F7; min-height: 100vh; padding-bottom: 200rpx; }
.loading { padding: 200rpx 0; text-align: center; color: #999; }
.load-error { padding: 200rpx 32rpx; text-align: center; color: #777;
  button { margin-top: 28rpx; width: 220rpx; min-height: 80rpx; border-radius: 40rpx; background: #FFF0EB; color: #D94F2B; }
}

.cover-wrap { position: relative;
  .cover { width: 100%; height: 400rpx; background: #f0f0f0; display: block; }
  .cover-fallback { display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 72rpx; color: #999;
    text { margin-top: 12rpx; font-size: 24rpx; }
  }
  .fav-btn {
    position: absolute; top: 24rpx; right: 24rpx;
    width: 80rpx; height: 80rpx; line-height: 80rpx;
    text-align: center; background: rgba(255,255,255,0.9);
    border-radius: 50%; font-size: 40rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.1);
  }
  .fav-btn.active { background: #fff; }
  .fav-btn.disabled { opacity: .6; }
}

.footer-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: #fff; padding: 16rpx 32rpx;
  display: flex; align-items: center; gap: 24rpx;
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.05);
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  .footer-fav { display: flex; flex-direction: column; align-items: center; padding: 8rpx 24rpx; font-size: 32rpx;
    .footer-fav-text { font-size: 20rpx; color: #999; margin-top: 4rpx; }
  }
  .footer-fav.disabled { opacity: .6; }
  .footer-btn { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 44rpx;
    background: #FF8866; color: #fff; font-size: 30rpx;
  }
  .footer-btn[disabled] { opacity: 0.6; }
}

.header { background: #fff; padding: 32rpx;
  .title { font-size: 36rpx; font-weight: 600; }
  .meta { display: flex; gap: 24rpx; color: #999; font-size: 26rpx; margin-top: 16rpx; }
  .tags { display: flex; gap: 12rpx; margin-top: 16rpx; flex-wrap: wrap;
    .tag { font-size: 22rpx; color: #FF6644; background: #FFE5DC; padding: 4rpx 16rpx; border-radius: 8rpx; }
  }
}

.card { background: #fff; margin-top: 24rpx; padding: 32rpx;
  .card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; color: #FF6644; }
}

.nutri-row { display: flex; justify-content: space-around;
  .nutri { text-align: center;
    .val { font-size: 32rpx; font-weight: bold; color: #FF6644; }
    .lbl { font-size: 22rpx; color: #999; margin-top: 8rpx; }
  }
}

.ing-list {
  .ing-row { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; border-bottom: 1rpx solid #f5f5f5;
    .qty { color: #999; }
  }
}
.allergen { margin-left: 10rpx; padding: 2rpx 8rpx; color: #A85D00; background: #FFF4E5; border-radius: 6rpx; font-size: 20rpx; }
.allergen-hint { margin-top: 20rpx; color: #777; font-size: 22rpx; line-height: 1.6; }

.step-row { display: flex; padding: 16rpx 0;
  .step-num { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; background: #FF8866; color: #fff; border-radius: 50%; font-size: 24rpx; margin-right: 24rpx; flex-shrink: 0; }
  .step-body { flex: 1;
    .step-desc { font-size: 28rpx; line-height: 1.6; }
    .step-timer { display: inline-block; margin-top: 12rpx; color: #FF6644; font-size: 24rpx; background: #FFE5DC; padding: 4rpx 16rpx; border-radius: 8rpx; }
  }
}

.disclaimer { text-align: center; color: #999; font-size: 22rpx; padding: 32rpx; }
.dialog-mask { position: fixed; inset: 0; z-index: 20; background: rgba(0,0,0,.45); display: flex; align-items: flex-end; }
.dialog { width: 100%; box-sizing: border-box; background: #fff; border-radius: 28rpx 28rpx 0 0; padding: 36rpx 32rpx calc(36rpx + env(safe-area-inset-bottom));
  .dialog-title { font-size: 32rpx; font-weight: 600; }
  .dialog-hint { margin-top: 12rpx; font-size: 23rpx; color: #777; line-height: 1.5; }
  .dialog-label { margin-top: 28rpx; margin-bottom: 12rpx; font-size: 25rpx; }
  .meal-options { display: flex; gap: 12rpx;
    .meal-option { flex: 1; padding: 16rpx 0; text-align: center; border: 1rpx solid #ddd; border-radius: 12rpx; font-size: 24rpx;
      &.active { color: #D94F2B; border-color: #FF8866; background: #FFF0EB; }
    }
  }
  .dialog-input { height: 72rpx; line-height: 72rpx; padding: 0 20rpx; border: 1rpx solid #ddd; border-radius: 12rpx; font-size: 26rpx; }
  .dialog-actions { display: flex; gap: 16rpx; margin-top: 32rpx;
    button { flex: 1; border-radius: 40rpx; }
    .dialog-cancel { background: #f2f2f2; color: #555; }
    .dialog-confirm { background: #FF8866; color: #fff; }
  }
}
.timer-mask { position: fixed; inset: 0; z-index: 30; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; padding: 32rpx; }
.timer-dialog { width: 100%; max-width: 620rpx; box-sizing: border-box; background: #fff; border-radius: 28rpx; padding: 44rpx 32rpx; text-align: center;
  .timer-title { font-size: 30rpx; font-weight: 600; }
  .timer-value { margin: 32rpx 0; color: #D94F2B; font-size: 72rpx; font-variant-numeric: tabular-nums;
    &.finished { font-size: 42rpx; }
  }
  .timer-actions { display: flex; gap: 16rpx;
    button { flex: 1; min-height: 88rpx; border-radius: 44rpx; font-size: 26rpx; }
    button:first-child { background: #FF8866; color: #fff; }
  }
}
</style>
