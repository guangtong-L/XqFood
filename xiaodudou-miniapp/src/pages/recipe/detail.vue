<template>
  <view class="page" v-if="data">
    <view class="cover-wrap">
      <image class="cover" :src="data.recipe.coverUrl" mode="aspectFill" />
      <view class="fav-btn" :class="{ active: favorited }" @tap="toggleFavorite">
        {{ favorited ? '❤️' : '🤍' }}
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
          <text>✅ {{ findIngName(ri.ingredientId) }}</text>
          <text class="qty">{{ ri.quantity }}</text>
        </view>
      </view>
      <button class="btn-buy">一键加入购物车</button>
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

    <view class="disclaimer">⚠️ 食谱仅供参考，特殊体质请咨询医生</view>

    <view class="footer-bar">
      <view class="footer-fav" @tap="toggleFavorite">
        <text>{{ favorited ? '❤️' : '🤍' }}</text>
        <text class="footer-fav-text">{{ favorited ? '已收藏' : '收藏' }}</text>
      </view>
      <button class="footer-btn" @tap="doCheckin" :disabled="checkinLoading">
        {{ checkinLoading ? '记录中...' : '✓ 我做了这道菜' }}
      </button>
    </view>
  </view>
  <view v-else class="loading">加载中...</view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { recipeApi, type RecipeDetail } from '../../api/recipe'
import { favoriteApi } from '../../api/favorite'
import { checkinApi } from '../../api/checkin'
import { feedback } from '../../utils/feedback'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const data = ref<RecipeDetail | null>(null)
const favorited = ref(false)
const checkinLoading = ref(false)

async function toggleFavorite() {
  if (!userStore.isLoggedIn) { feedback.toast('请先登录'); return }
  if (!data.value) return
  const id = data.value.recipe.id
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
  } catch (e) { console.error(e) }
}

async function doCheckin() {
  if (!userStore.isLoggedIn) { feedback.toast('请先登录'); return }
  if (!data.value) return
  checkinLoading.value = true
  try {
    await checkinApi.checkin(data.value.recipe.id)
    feedback.success('打卡成功')
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

function formatTimer(sec: number) {
  if (sec < 60) return sec + ' 秒'
  return Math.floor(sec / 60) + ' 分' + (sec % 60 ? (sec % 60) + ' 秒' : '')
}

function startTimer(sec: number) {
  uni.showToast({ title: `计时 ${formatTimer(sec)}`, icon: 'none' })
  // M2 实现真正计时器
}

onLoad(async (q: any) => {
  if (!q?.id) {
    feedback.toast('参数缺失')
    return
  }
  try {
    data.value = await recipeApi.detail(q.id)
    // 已登录就查收藏状态
    if (userStore.isLoggedIn) {
      try { favorited.value = await favoriteApi.check(q.id) } catch {}
    }
  } catch (e) { console.error(e) }
})
</script>

<style lang="scss" scoped>
.page { background: #F7F7F7; min-height: 100vh; padding-bottom: 200rpx; }
.loading { padding: 200rpx 0; text-align: center; color: #999; }

.cover-wrap { position: relative;
  .cover { width: 100%; height: 400rpx; background: #f0f0f0; display: block; }
  .fav-btn {
    position: absolute; top: 24rpx; right: 24rpx;
    width: 80rpx; height: 80rpx; line-height: 80rpx;
    text-align: center; background: rgba(255,255,255,0.9);
    border-radius: 50%; font-size: 40rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.1);
  }
  .fav-btn.active { background: #fff; }
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
.btn-buy { margin-top: 24rpx; background: #FFE5DC; color: #FF6644; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; font-size: 26rpx; }

.step-row { display: flex; padding: 16rpx 0;
  .step-num { width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center; background: #FF8866; color: #fff; border-radius: 50%; font-size: 24rpx; margin-right: 24rpx; flex-shrink: 0; }
  .step-body { flex: 1;
    .step-desc { font-size: 28rpx; line-height: 1.6; }
    .step-timer { display: inline-block; margin-top: 12rpx; color: #FF6644; font-size: 24rpx; background: #FFE5DC; padding: 4rpx 16rpx; border-radius: 8rpx; }
  }
}

.disclaimer { text-align: center; color: #999; font-size: 22rpx; padding: 32rpx; }
</style>
