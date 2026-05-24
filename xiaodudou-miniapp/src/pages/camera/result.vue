<template>
  <view class="page">
    <!-- 空状态 -->
    <view v-if="ingredients.length === 0" class="empty">
      <view class="empty-emoji">🥗</view>
      <view class="empty-title">{{ isManual ? '手动添加食材' : '没识别到食材' }}</view>
      <view v-if="!isManual" class="empty-tips">
        <view>· 食材尽量平铺、避免重叠</view>
        <view>· 光线充足、对焦清晰</view>
        <view>· 一次拍 3-8 种最佳</view>
      </view>
      <view class="empty-actions">
        <button class="btn-default" @tap="goBack" v-if="!isManual">📷 重拍</button>
        <button class="btn-primary" @tap="showAddDialog = true">+ 添加食材</button>
      </view>
      <view class="alt-skip" @tap="skipToRecommend">跳过，直接看推荐 →</view>
    </view>

    <!-- 有食材时的网格 -->
    <view v-else>
      <view class="header">
        识别到 {{ ingredients.length }} 种食材
        <text class="header-sub">点 × 删除，点 + 添加</text>
      </view>

      <view class="ingredient-grid">
        <view class="ingredient-card" v-for="(item, idx) in ingredients" :key="idx">
          <view class="emoji">{{ item.emoji || '🥗' }}</view>
          <view class="name">{{ item.name }}</view>
          <view v-if="item.quantityEstimate" class="qty">{{ item.quantityEstimate }}</view>
          <view class="del" @tap="remove(idx)">×</view>
        </view>
        <view class="ingredient-card add" @tap="showAddDialog = true">
          <view class="emoji">+</view>
          <view class="name">添加</view>
        </view>
      </view>

      <view class="stage-info">适配：{{ stageDesc }}</view>

      <button class="btn-recommend" :disabled="recommending" @tap="goRecommend">
        🤖 让 AI 推荐菜谱（{{ ingredients.length }} 种食材）
      </button>
    </view>

    <!-- 添加食材弹窗 -->
    <view v-if="showAddDialog" class="dialog-mask" @tap.self="showAddDialog = false">
      <view class="dialog">
        <view class="dialog-title">添加食材</view>
        <input class="dialog-input" v-model="newIngName" placeholder="如：菠菜" focus />
        <view class="quick-tags">
          <text v-for="t in QUICK_TAGS" :key="t.name"
                class="quick-tag" @tap="quickPick(t)">
            {{ t.emoji }} {{ t.name }}
          </text>
        </view>
        <view class="dialog-actions">
          <button class="btn-default" @tap="showAddDialog = false">取消</button>
          <button class="btn-primary" @tap="addIngredient">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { RecognizedIngredient } from '../../api/ai'
import { useUserStore } from '../../store/user'
import { feedback } from '../../utils/feedback'

const userStore = useUserStore()
const ingredients = ref<Array<Partial<RecognizedIngredient>>>([])
const recommending = ref(false)
const showAddDialog = ref(false)
const newIngName = ref('')
const isManual = ref(false)

// 快速添加常用食材
const QUICK_TAGS = [
  { name: '番茄', emoji: '🍅' }, { name: '鸡蛋', emoji: '🥚' },
  { name: '猪肉', emoji: '🥩' }, { name: '青菜', emoji: '🥬' },
  { name: '土豆', emoji: '🥔' }, { name: '胡萝卜', emoji: '🥕' },
  { name: '鱼', emoji: '🐟' }, { name: '虾', emoji: '🦐' }
]

const stageDesc = computed(() => {
  const p = userStore.profile
  if (!p) return '默认阶段'
  if (p.stageType === 'POSTPARTUM') return `月子第 ${p.postpartumDay ?? '?'} 天`
  if (p.stageType === 'PREGNANCY') return `孕 ${p.pregnancyWeek ?? '?'} 周`
  if (p.stageType === 'WEANING') return '辅食期'
  return p.stageType
})

onLoad((q: any) => {
  if (q?.manual === '1') {
    isManual.value = true
    ingredients.value = []
  } else {
    const cached = uni.getStorageSync('recognized_ingredients')
    ingredients.value = cached || []
  }
})

function goBack() { uni.navigateBack() }

function remove(idx: number) {
  ingredients.value.splice(idx, 1)
}

function quickPick(tag: { name: string; emoji: string }) {
  if (ingredients.value.some(i => i.name === tag.name)) {
    feedback.toast('已添加')
    return
  }
  ingredients.value.push({ name: tag.name, emoji: tag.emoji })
  showAddDialog.value = false
  newIngName.value = ''
}

function addIngredient() {
  const name = newIngName.value.trim()
  if (!name) { feedback.toast('请输入食材名'); return }
  if (ingredients.value.some(i => i.name === name)) { feedback.toast('已添加'); return }
  ingredients.value.push({ name, emoji: '🥗' })
  newIngName.value = ''
  showAddDialog.value = false
}

async function goRecommend() {
  if (ingredients.value.length === 0) {
    feedback.toast('请先添加食材')
    return
  }
  // 把食材带到推荐页（推荐页负责调 API）
  uni.setStorageSync('recommend_input', {
    stage: userStore.profile || { stageType: 'POSTPARTUM', postpartumDay: 12 },
    ingredients: ingredients.value.map(i => ({ name: i.name, quantity: i.quantityEstimate })),
    constraints: {
      allergies: userStore.profile?.allergies,
      dislikes: userStore.profile?.dislikes,
      maxCookMinutes: 60
    }
  })
  uni.removeStorageSync('ai_recommendations') // 清旧结果让推荐页重新调
  uni.navigateTo({ url: '/pages/camera/recommend' })
}

function skipToRecommend() {
  // 跳过：用阶段默认参数推荐
  uni.setStorageSync('recommend_input', {
    stage: userStore.profile || { stageType: 'POSTPARTUM', postpartumDay: 12 },
    ingredients: [],
    constraints: { allergies: userStore.profile?.allergies, maxCookMinutes: 60 }
  })
  uni.removeStorageSync('ai_recommendations')
  uni.navigateTo({ url: '/pages/camera/recommend' })
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; background: #fff; min-height: 100vh; }

.empty {
  padding: 120rpx 32rpx 32rpx;
  text-align: center;
  .empty-emoji { font-size: 160rpx; opacity: 0.6; }
  .empty-title { font-size: 32rpx; font-weight: 600; margin-top: 32rpx; }
  .empty-tips {
    background: #FFF8F5; border-radius: 16rpx;
    padding: 24rpx 32rpx; margin: 32rpx 0;
    font-size: 26rpx; color: #666; text-align: left;
    view { padding: 8rpx 0; }
  }
  .empty-actions {
    display: flex; gap: 24rpx; margin: 48rpx 0 24rpx;
    button { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; }
    .btn-default { background: #F7F7F7; color: #333; }
    .btn-primary { background: #FF8866; color: #fff; }
  }
  .alt-skip { color: #999; font-size: 24rpx; padding: 24rpx; }
}

.header { font-size: 30rpx; font-weight: 600; margin-bottom: 32rpx;
  .header-sub { font-size: 22rpx; color: #999; font-weight: normal; margin-left: 16rpx; }
}

.ingredient-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16rpx; margin-bottom: 48rpx; }
.ingredient-card { background: #FFF8F5; border-radius: 16rpx; padding: 24rpx 8rpx; text-align: center; position: relative;
  .emoji { font-size: 48rpx; }
  .name { font-size: 24rpx; margin-top: 8rpx; }
  .qty { font-size: 20rpx; color: #999; margin-top: 4rpx; }
  .del { position: absolute; top: -8rpx; right: -8rpx; width: 36rpx; height: 36rpx; line-height: 32rpx;
    background: #FF8866; color: #fff; border-radius: 50%; font-size: 24rpx; text-align: center; }
  &.add { background: #F7F7F7; color: #999; }
}

.dialog-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.dialog { width: 600rpx; background: #fff; border-radius: 24rpx; padding: 48rpx 32rpx;
  .dialog-title { font-size: 32rpx; font-weight: 600; margin-bottom: 32rpx; text-align: center; }
  .dialog-input { padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 28rpx; margin-bottom: 24rpx; }
  .quick-tags { display: flex; flex-wrap: wrap; gap: 12rpx; margin-bottom: 32rpx;
    .quick-tag { font-size: 24rpx; background: #FFE5DC; color: #FF6644; padding: 8rpx 16rpx; border-radius: 20rpx; }
  }
  .dialog-actions { display: flex; gap: 24rpx;
    button { flex: 1; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; font-size: 26rpx; }
    .btn-default { background: #F7F7F7; color: #333; }
    .btn-primary { background: #FF8866; color: #fff; }
  }
}

.stage-info { text-align: center; color: #999; font-size: 26rpx; margin: 32rpx 0; }
.btn-recommend { background: #FF8866; color: #fff; height: 96rpx; line-height: 96rpx; border-radius: 48rpx; font-size: 30rpx; }
.btn-recommend[disabled] { opacity: 0.6; }
</style>
