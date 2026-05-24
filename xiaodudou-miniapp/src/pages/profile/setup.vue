<template>
  <view class="page">
    <view class="header">
      <view class="title">完善信息（{{ step }}/4）</view>
      <view class="progress"><view class="bar" :style="{ width: (step * 25) + '%' }"></view></view>
    </view>

    <!-- Step 1: 阶段 -->
    <view v-if="step === 1" class="step">
      <view class="q">你目前处于哪个阶段？</view>
      <view class="opts">
        <view class="opt" :class="{ active: form.stageType === 'PREPARE' }" @tap="form.stageType = 'PREPARE'">⚪ 备孕</view>
        <view class="opt" :class="{ active: form.stageType === 'PREGNANCY' }" @tap="form.stageType = 'PREGNANCY'">🤰 孕期</view>
        <view class="opt" :class="{ active: form.stageType === 'POSTPARTUM' }" @tap="form.stageType = 'POSTPARTUM'">🤱 月子/哺乳</view>
        <view class="opt" :class="{ active: form.stageType === 'WEANING' }" @tap="form.stageType = 'WEANING'">🍼 辅食期</view>
        <view class="opt" :class="{ active: form.stageType === 'CHILD' }" @tap="form.stageType = 'CHILD'">🧒 儿童餐</view>
      </view>
    </view>

    <!-- Step 2: 详细 -->
    <view v-else-if="step === 2" class="step">
      <view class="q">告诉我们更多</view>

      <view v-if="form.stageType === 'PREGNANCY'" class="field">
        <view class="label">孕周</view>
        <input class="input" type="number" v-model="form.pregnancyWeek" placeholder="如 28" />
      </view>

      <view v-else-if="form.stageType === 'POSTPARTUM'" class="field">
        <view class="label">产后天数</view>
        <input class="input" type="number" v-model="form.postpartumDay" placeholder="如 12" />
        <view class="label">分娩方式</view>
        <view class="radio-row">
          <view class="radio" :class="{ active: form.deliveryType === 'natural' }" @tap="form.deliveryType = 'natural'">顺产</view>
          <view class="radio" :class="{ active: form.deliveryType === 'cesarean' }" @tap="form.deliveryType = 'cesarean'">剖宫产</view>
        </view>
        <view class="label">喂养方式</view>
        <view class="radio-row">
          <view class="radio" :class="{ active: form.feedingType === 'breast' }" @tap="form.feedingType = 'breast'">母乳</view>
          <view class="radio" :class="{ active: form.feedingType === 'mixed' }" @tap="form.feedingType = 'mixed'">混合</view>
          <view class="radio" :class="{ active: form.feedingType === 'formula' }" @tap="form.feedingType = 'formula'">奶粉</view>
        </view>
      </view>

      <view v-else-if="form.stageType === 'WEANING' || form.stageType === 'CHILD'" class="field">
        <view class="label">宝宝出生日期</view>
        <picker mode="date" :value="form.babyBirthDate" @change="(e: any) => form.babyBirthDate = e.detail.value">
          <view class="input">{{ form.babyBirthDate || '点击选择' }}</view>
        </picker>
      </view>

      <view v-else class="hint">这一步可跳过</view>
    </view>

    <!-- Step 3: 过敏 -->
    <view v-else-if="step === 3" class="step">
      <view class="q">有什么过敏源吗？（多选）</view>
      <view class="tags">
        <view v-for="a in ALLERGENS" :key="a.value"
              class="tag" :class="{ active: form.allergies?.includes(a.value) }"
              @tap="toggleAllergy(a.value)">
          {{ a.label }}
        </view>
      </view>
    </view>

    <!-- Step 4: 忌口 -->
    <view v-else-if="step === 4" class="step">
      <view class="q">还有什么不爱吃的？</view>
      <textarea class="textarea" v-model="dislikesText" placeholder="如：香菜 / 内脏 / 苦瓜，用顿号分隔" />
    </view>

    <view class="actions">
      <button v-if="step > 1" class="btn-default" @tap="step--">上一步</button>
      <button v-if="step < 4" class="btn-primary" :disabled="!canNext" @tap="step++">下一步</button>
      <button v-else class="btn-primary" :disabled="saving" @tap="save">{{ saving ? '保存中...' : '完成' }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { userApi, type UserProfile } from '../../api/user'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const step = ref(1)
const saving = ref(false)
const dislikesText = ref('')

const form = ref<UserProfile>({
  stageType: 'POSTPARTUM',
  postpartumDay: 12,
  deliveryType: 'natural',
  feedingType: 'breast',
  allergies: [],
  dislikes: []
})

const ALLERGENS = [
  { label: '鸡蛋', value: 'egg' },
  { label: '牛奶', value: 'milk' },
  { label: '海鲜', value: 'seafood' },
  { label: '花生', value: 'peanut' },
  { label: '坚果', value: 'nut' },
  { label: '大豆', value: 'soy' },
  { label: '小麦', value: 'wheat' },
  { label: '芒果', value: 'mango' }
]

const canNext = computed(() => {
  if (step.value === 1) return !!form.value.stageType
  if (step.value === 2) {
    if (form.value.stageType === 'POSTPARTUM') return !!form.value.postpartumDay && !!form.value.deliveryType && !!form.value.feedingType
    if (form.value.stageType === 'PREGNANCY') return !!form.value.pregnancyWeek
    return true
  }
  return true
})

function toggleAllergy(v: string) {
  const arr = form.value.allergies || []
  const idx = arr.indexOf(v)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(v)
  form.value.allergies = [...arr]
}

async function save() {
  saving.value = true
  try {
    form.value.dislikes = dislikesText.value
      .split(/[、,，\s]+/)
      .map(s => s.trim())
      .filter(Boolean)

    const saved = await userApi.saveProfile(form.value)
    userStore.setProfile(saved)
    uni.showToast({ title: '已保存', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 600)
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; min-height: 100vh; background: #fff; display: flex; flex-direction: column; }
.header { margin-bottom: 48rpx;
  .title { font-size: 36rpx; font-weight: bold; margin-bottom: 16rpx; }
  .progress { height: 8rpx; background: #f0f0f0; border-radius: 4rpx; overflow: hidden;
    .bar { height: 100%; background: linear-gradient(90deg, #FF8866, #FFB199); transition: width .3s; }
  }
}
.step { flex: 1; }
.q { font-size: 32rpx; font-weight: bold; margin-bottom: 32rpx; }
.opts {
  .opt { padding: 32rpx; background: #FFF8F5; border-radius: 16rpx; margin-bottom: 16rpx; font-size: 30rpx; border: 2rpx solid transparent;
    &.active { border-color: #FF8866; background: #FFE5DC; color: #FF6644; font-weight: 500; }
  }
}
.field {
  .label { font-size: 28rpx; color: #666; margin: 24rpx 0 12rpx; }
  .input { padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 30rpx; }
}
.radio-row { display: flex; gap: 16rpx;
  .radio { flex: 1; text-align: center; padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; border: 2rpx solid transparent;
    &.active { border-color: #FF8866; background: #FFE5DC; color: #FF6644; }
  }
}
.tags { display: flex; flex-wrap: wrap; gap: 16rpx;
  .tag { padding: 20rpx 32rpx; background: #F7F7F7; border-radius: 40rpx; font-size: 28rpx; border: 2rpx solid transparent;
    &.active { background: #FFE5DC; border-color: #FF8866; color: #FF6644; }
  }
}
.textarea { width: 100%; min-height: 200rpx; padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.hint { color: #999; font-size: 28rpx; }
.actions { display: flex; gap: 24rpx; padding-top: 48rpx;
  .btn-default, .btn-primary { flex: 1; height: 96rpx; line-height: 96rpx; border-radius: 48rpx; font-size: 30rpx; }
  .btn-default { background: #F7F7F7; color: #333; }
  .btn-primary { background: #FF8866; color: #fff; }
  .btn-primary[disabled] { opacity: 0.5; }
}
</style>
