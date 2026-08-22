<template>
  <view class="page">
    <view class="header">
      <view class="title">完善营养画像（{{ step }}/5）</view>
      <view class="progress"><view class="bar" :style="{ width: (step * 20) + '%' }" /></view>
    </view>

    <view v-if="step === 1" class="step">
      <view class="q">敏感信息用途说明</view>
      <view class="privacy-card">
        <view>我们将收集母婴阶段、过敏、忌口和您主动填写的健康备注，用于阶段信息展示，以及在相关功能开放后进行菜谱标签冲突提示。</view>
        <view>这些资料会加密保存，不用于妈妈圈公开展示，也不能替代医生诊断或治疗建议。</view>
        <view>您可以稍后填写，也可以在设置中注销账号并删除相关画像。</view>
      </view>
      <view class="consent" :class="{ active: consent }" @tap="consent = !consent">
        <view class="checkbox">{{ consent ? '✓' : '' }}</view>
        <text>我已阅读并单独同意上述敏感信息用途</text>
      </view>
    </view>

    <view v-else-if="step === 2" class="step">
      <view class="q">你目前处于哪个阶段？</view>
      <view class="opts">
        <view v-for="item in STAGES" :key="item.value" class="opt"
              :class="{ active: form.stageType === item.value }" @tap="selectStage(item.value)">
          {{ item.icon }} {{ item.label }}
        </view>
      </view>
    </view>

    <view v-else-if="step === 3" class="step">
      <view class="q">补充阶段信息</view>
      <view v-if="form.stageType === 'PREGNANCY'" class="field">
        <view class="label">孕周（1–42）</view>
        <input class="input" type="number" :value="form.pregnancyWeek ?? ''"
               placeholder="请选择真实孕周" @input="form.pregnancyWeek = numberValue($event)" />
      </view>
      <view v-else-if="form.stageType === 'POSTPARTUM'" class="field">
        <view class="label">产后天数（0–730）</view>
        <input class="input" type="number" :value="form.postpartumDay ?? ''"
               placeholder="请输入产后天数" @input="form.postpartumDay = numberValue($event)" />
        <view class="label">分娩方式（请选择）</view>
        <view class="radio-row">
          <view class="radio" :class="{ active: form.deliveryType === 'natural' }" @tap="form.deliveryType = 'natural'">顺产</view>
          <view class="radio" :class="{ active: form.deliveryType === 'cesarean' }" @tap="form.deliveryType = 'cesarean'">剖宫产</view>
        </view>
        <view class="label">喂养方式（请选择）</view>
        <view class="radio-row">
          <view class="radio" :class="{ active: form.feedingType === 'breast' }" @tap="form.feedingType = 'breast'">母乳</view>
          <view class="radio" :class="{ active: form.feedingType === 'mixed' }" @tap="form.feedingType = 'mixed'">混合</view>
          <view class="radio" :class="{ active: form.feedingType === 'formula' }" @tap="form.feedingType = 'formula'">配方奶</view>
        </view>
      </view>
      <view v-else-if="form.stageType === 'WEANING' || form.stageType === 'CHILD'" class="field">
        <view class="label">宝宝出生日期</view>
        <picker mode="date" :start="birthDateStart" :end="today" :value="form.babyBirthDate || ''" @change="onBirthDate">
          <view class="input">{{ form.babyBirthDate || '点击选择' }}</view>
        </picker>
      </view>
      <view v-else class="hint">备孕阶段无需填写其他阶段字段。</view>
    </view>

    <view v-else-if="step === 4" class="step">
      <view class="q">请选择已知过敏源（可不选）</view>
      <view class="hint-block">用于降低已知标签冲突风险，不能保证排除全部过敏源；不确定时请勿猜测。</view>
      <view class="tags">
        <view v-for="item in ALLERGENS" :key="item.value" class="tag"
              :class="{ active: form.allergies?.includes(item.value) }" @tap="toggle('allergies', item.value)">
          {{ item.label }}
        </view>
      </view>
    </view>

    <view v-else class="step">
      <view class="q">忌口与健康备注（均可不填）</view>
      <view class="label">不喜欢或需要避开的食物</view>
      <view class="tags">
        <view v-for="item in DISLIKES" :key="item.value" class="tag"
              :class="{ active: form.dislikes?.includes(item.value) }" @tap="toggle('dislikes', item.value)">
          {{ item.label }}
        </view>
      </view>
      <view class="label health-label">健康备注（最多 500 字）</view>
      <textarea class="textarea" v-model="form.healthNotes" maxlength="500"
                placeholder="仅填写与饮食推荐有关的信息，请勿填写姓名、手机号等身份信息" />
      <view class="counter">{{ form.healthNotes?.length || 0 }}/500</view>
    </view>

    <view v-if="errorText" class="error">{{ errorText }}</view>
    <view class="actions">
      <button class="btn-later" @tap="leave">返回/稍后填写</button>
      <button v-if="step > 1" class="btn-default" @tap="previous">上一步</button>
      <button v-if="step < 5" class="btn-primary" @tap="next">下一步</button>
      <button v-else class="btn-primary" :disabled="saving" @tap="save">{{ saving ? '保存中...' : '确认保存' }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { userApi, type SaveProfileRequest, type UserProfile } from '../../api/user'
import { useUserStore } from '../../store/user'
import { showRequestError } from '../../utils/request'

type StageType = UserProfile['stageType']
type MultiField = 'allergies' | 'dislikes'

const userStore = useUserStore()
const step = ref(1)
const saving = ref(false)
const consent = ref(false)
const errorText = ref('')
const form = ref<Partial<UserProfile>>({ allergies: [], dislikes: [] })
const today = computed(() => new Date().toISOString().slice(0, 10))
const birthDateStart = computed(() => {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 18)
  return date.toISOString().slice(0, 10)
})

const STAGES: Array<{ label: string; icon: string; value: StageType }> = [
  { label: '备孕', icon: '⚪', value: 'PREPARE' },
  { label: '孕期', icon: '🤰', value: 'PREGNANCY' },
  { label: '产后/哺乳', icon: '🤱', value: 'POSTPARTUM' },
  { label: '辅食期', icon: '🍼', value: 'WEANING' },
  { label: '儿童餐', icon: '🧒', value: 'CHILD' }
]

const ALLERGENS = [
  ['鸡蛋', 'egg'], ['牛奶', 'milk'], ['海鲜', 'seafood'], ['花生', 'peanut'],
  ['坚果', 'nut'], ['大豆', 'soy'], ['小麦', 'wheat'], ['芒果', 'mango']
].map(([label, value]) => ({ label, value }))

const DISLIKES = [
  ['香菜', 'coriander'], ['内脏', 'offal'], ['苦瓜', 'bitter_melon'], ['芹菜', 'celery'],
  ['姜', 'ginger'], ['蒜', 'garlic'], ['洋葱', 'onion'], ['菌菇', 'mushroom'],
  ['胡萝卜', 'carrot'], ['茄子', 'eggplant'], ['肥肉', 'fat_meat'], ['辛辣', 'spicy']
].map(([label, value]) => ({ label, value }))

onLoad(async () => {
  try {
    const existing = await userApi.getProfile()
    if (existing) form.value = { ...existing, allergies: [...(existing.allergies || [])], dislikes: [...(existing.dislikes || [])] }
  } catch { /* 请求层已展示错误，用户仍可返回 */ }
})

function selectStage(stage: StageType) {
  form.value.stageType = stage
  form.value.pregnancyWeek = undefined
  form.value.postpartumDay = undefined
  form.value.deliveryType = undefined
  form.value.feedingType = undefined
  form.value.babyBirthDate = undefined
  errorText.value = ''
}

function numberValue(event: { detail: { value: string } }) {
  if (event.detail.value === '') return undefined
  const value = Number(event.detail.value)
  return Number.isFinite(value) ? value : undefined
}

function onBirthDate(event: { detail: { value: string } }) {
  form.value.babyBirthDate = event.detail.value
}

function toggle(field: MultiField, value: string) {
  const values = [...(form.value[field] || [])]
  const index = values.indexOf(value)
  if (index >= 0) values.splice(index, 1)
  else values.push(value)
  form.value[field] = values
}

function validateCurrent() {
  if (step.value === 1 && !consent.value) return '请先阅读并单独确认敏感信息用途'
  if (step.value === 2 && !form.value.stageType) return '请选择当前母婴阶段'
  if (step.value === 3) {
    if (form.value.stageType === 'PREGNANCY' && (!form.value.pregnancyWeek || form.value.pregnancyWeek < 1 || form.value.pregnancyWeek > 42)) return '请输入 1–42 的合法孕周'
    if (form.value.stageType === 'POSTPARTUM') {
      if (form.value.postpartumDay === undefined || form.value.postpartumDay < 0 || form.value.postpartumDay > 730) return '请输入 0–730 的产后天数'
      if (!form.value.deliveryType) return '请选择分娩方式'
      if (!form.value.feedingType) return '请选择喂养方式'
    }
    if ((form.value.stageType === 'WEANING' || form.value.stageType === 'CHILD') && !form.value.babyBirthDate) return '请选择宝宝出生日期'
    if (form.value.babyBirthDate && form.value.babyBirthDate > today.value) return '宝宝出生日期不能晚于今天'
    if (form.value.babyBirthDate && form.value.babyBirthDate < birthDateStart.value) return '宝宝年龄必须在 0–18 周岁范围内'
  }
  return ''
}

function next() {
  const message = validateCurrent()
  if (message) { errorText.value = message; return }
  errorText.value = ''
  step.value += 1
}

function previous() {
  errorText.value = ''
  step.value -= 1
}

function leave() {
  uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}

async function save() {
  const message = validateCurrent()
  if (message) { errorText.value = message; return }
  if (!consent.value || !form.value.stageType) { errorText.value = '请完成用途确认和阶段选择'; return }
  saving.value = true
  try {
    const request: SaveProfileRequest = {
      stageType: form.value.stageType,
      pregnancyWeek: form.value.pregnancyWeek,
      postpartumDay: form.value.postpartumDay,
      deliveryType: form.value.deliveryType,
      feedingType: form.value.feedingType,
      babyBirthDate: form.value.babyBirthDate,
      allergies: form.value.allergies || [],
      dislikes: form.value.dislikes || [],
      healthNotes: form.value.healthNotes?.trim() || undefined,
      sensitiveInfoConsent: true
    }
    const saved = await userApi.saveProfile(request)
    userStore.setProfile(saved)
    uni.showToast({ title: '画像已保存', icon: 'success' })
    setTimeout(leave, 500)
  } catch (error) {
    showRequestError(error, '保存失败，请检查填写内容')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; min-height: 100vh; background: #fff; display: flex; flex-direction: column; box-sizing: border-box; }
.header { margin-bottom: 40rpx;
  .title { font-size: 34rpx; font-weight: 600; margin-bottom: 16rpx; }
  .progress { height: 8rpx; background: #f0f0f0; border-radius: 4rpx; overflow: hidden;
    .bar { height: 100%; background: linear-gradient(90deg, #FF8866, #FFB199); transition: width .3s; }
  }
}
.step { flex: 1; }
.q { font-size: 32rpx; font-weight: 600; margin-bottom: 28rpx; }
.privacy-card { padding: 28rpx; border-radius: 16rpx; background: #FFF8F5; color: #555; font-size: 26rpx; line-height: 1.75;
  view + view { margin-top: 16rpx; }
}
.consent { display: flex; gap: 16rpx; align-items: flex-start; padding: 28rpx 8rpx; font-size: 27rpx; color: #555;
  .checkbox { flex: none; width: 38rpx; height: 38rpx; line-height: 38rpx; text-align: center; border: 2rpx solid #bbb; border-radius: 8rpx; }
  &.active { color: #333; .checkbox { color: #fff; background: #FF8866; border-color: #FF8866; } }
}
.opts .opt { padding: 28rpx; background: #FFF8F5; border-radius: 16rpx; margin-bottom: 16rpx; font-size: 29rpx; border: 2rpx solid transparent;
  &.active { border-color: #FF8866; background: #FFE5DC; color: #FF6644; font-weight: 500; }
}
.label { font-size: 27rpx; color: #555; margin: 24rpx 0 12rpx; }
.input { padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 29rpx; }
.radio-row { display: flex; gap: 16rpx;
  .radio { flex: 1; text-align: center; padding: 22rpx 12rpx; background: #F7F7F7; border-radius: 12rpx; border: 2rpx solid transparent;
    &.active { border-color: #FF8866; background: #FFE5DC; color: #FF6644; }
  }
}
.hint, .hint-block { color: #777; font-size: 25rpx; line-height: 1.6; }
.hint-block { background: #F7F7F7; padding: 20rpx; border-radius: 12rpx; margin-bottom: 28rpx; }
.tags { display: flex; flex-wrap: wrap; gap: 16rpx;
  .tag { padding: 18rpx 28rpx; background: #F7F7F7; border-radius: 36rpx; font-size: 27rpx; border: 2rpx solid transparent;
    &.active { background: #FFE5DC; border-color: #FF8866; color: #FF6644; }
  }
}
.health-label { margin-top: 36rpx; }
.textarea { width: 100%; min-height: 190rpx; padding: 22rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 27rpx; box-sizing: border-box; }
.counter { text-align: right; color: #999; font-size: 22rpx; margin-top: 8rpx; }
.error { color: #D83A2E; background: #FFF0EE; border-radius: 12rpx; padding: 18rpx 22rpx; font-size: 25rpx; margin-top: 20rpx; }
.actions { display: flex; flex-wrap: wrap; gap: 16rpx; padding-top: 32rpx;
  button { height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 27rpx; margin: 0; }
  .btn-later { width: 100%; background: transparent; color: #777; border: 1rpx solid #ddd; }
  .btn-default, .btn-primary { flex: 1; }
  .btn-default { background: #F7F7F7; color: #333; }
  .btn-primary { background: #FF8866; color: #fff; }
  .btn-primary[disabled] { opacity: .5; }
}
</style>
