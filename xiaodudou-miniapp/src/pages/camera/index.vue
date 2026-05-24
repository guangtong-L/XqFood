<template>
  <view class="page">
    <view class="hint">把冰箱内食材尽量平铺，AI 识别更准</view>

    <view class="preview-frame">
      <image v-if="imgUrl" :src="imgUrl" mode="aspectFit" class="preview-img" />
      <view v-else class="preview-placeholder">📷</view>
    </view>

    <view class="btn-group">
      <button class="btn btn-default" @tap="chooseFromAlbum" :disabled="loading">📁 相册</button>
      <button class="btn btn-primary" @tap="takePhoto" :disabled="loading">📷 拍照</button>
    </view>

    <view class="alt-entry" @tap="manualInput">或：手动输入食材 →</view>

    <!-- AI 识别 loading -->
    <view v-if="loading" class="loading-mask" @tap.stop>
      <view class="loading-card">
        <view class="loading-icon">🤖</view>
        <view class="loading-title">AI 识别中</view>
        <view class="loading-sub">{{ progressText }}</view>
        <view class="progress-bar">
          <view class="progress-inner" :style="{ width: progress + '%' }"></view>
        </view>
        <view class="loading-time">已等待 {{ elapsedSec }} s</view>
        <view class="loading-tip">智谱 GLM-4V 通常需要 15-40 秒</view>
        <view class="loading-cancel" @tap="cancelUpload">取消并手动添加</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { aiApi } from '../../api/ai'
import { useUserStore } from '../../store/user'
import { feedback } from '../../utils/feedback'

const imgUrl = ref('')
const loading = ref(false)
const progress = ref(0)
const progressText = ref('准备上传...')
const elapsedSec = ref(0)

const userStore = useUserStore()
let progressTimer: ReturnType<typeof setInterval> | null = null
let elapsedTimer: ReturnType<typeof setInterval> | null = null
let cancelled = false

function chooseFromAlbum() {
  uni.chooseImage({
    count: 1, sourceType: ['album'],
    success: (res) => { imgUrl.value = res.tempFilePaths[0]; upload(res.tempFilePaths[0]) }
  })
}

function takePhoto() {
  uni.chooseImage({
    count: 1, sourceType: ['camera'],
    success: (res) => { imgUrl.value = res.tempFilePaths[0]; upload(res.tempFilePaths[0]) }
  })
}

function manualInput() {
  uni.navigateTo({ url: '/pages/camera/result?manual=1' })
}

function startProgress() {
  progress.value = 0
  elapsedSec.value = 0
  progressText.value = '上传图片中...'
  cancelled = false

  // 进度条：到 90% 停，留给真实完成跳 100%
  progressTimer = setInterval(() => {
    if (progress.value < 90) {
      progress.value = Math.min(90, progress.value + (progress.value < 50 ? 4 : progress.value < 80 ? 2 : 0.6))
      if (progress.value < 15) progressText.value = '上传图片中...'
      else if (progress.value < 40) progressText.value = 'AI 视觉模型识别中...'
      else if (progress.value < 65) progressText.value = '整理食材清单...'
      else if (progress.value < 80) progressText.value = '匹配阶段营养需求...'
      else progressText.value = '马上好了...'
    }
  }, 500)

  // 计时
  elapsedTimer = setInterval(() => { elapsedSec.value++ }, 1000)
}

function stopProgress(success: boolean) {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
  if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null }
  if (success) {
    progress.value = 100
    progressText.value = '识别完成'
  }
}

function cancelUpload() {
  cancelled = true
  stopProgress(false)
  loading.value = false
  feedback.toast('已取消，请手动添加食材')
  uni.navigateTo({ url: '/pages/camera/result?manual=1' })
}

async function upload(filePath: string) {
  if (!userStore.isLoggedIn) {
    feedback.toast('请先登录')
    setTimeout(() => uni.navigateTo({ url: '/pages/auth/wx-login' }), 600)
    return
  }
  loading.value = true
  startProgress()

  try {
    const stageHint = userStore.profile?.stageType
    const data = await aiApi.recognize(filePath, stageHint)

    if (cancelled) return  // 用户已取消，不再跳转

    stopProgress(true)
    uni.setStorageSync('recognized_ingredients', data.ingredients)
    setTimeout(() => {
      loading.value = false
      uni.navigateTo({ url: '/pages/camera/result' })
    }, 300)
  } catch (e) {
    stopProgress(false)
    loading.value = false
    if (!cancelled) {
      feedback.error('识别失败，请手动添加')
      setTimeout(() => uni.navigateTo({ url: '/pages/camera/result?manual=1' }), 800)
    }
  }
}

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
  if (elapsedTimer) clearInterval(elapsedTimer)
})
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; min-height: 100vh; background: #fff; }
.hint { text-align: center; font-size: 26rpx; color: #999; margin-bottom: 32rpx; }
.preview-frame { width: 100%; height: 600rpx; background: #F7F7F7; border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center; margin-bottom: 48rpx; overflow: hidden;
  .preview-img { width: 100%; height: 100%; }
  .preview-placeholder { font-size: 120rpx; opacity: 0.3; }
}
.btn-group { display: flex; gap: 24rpx; margin-bottom: 32rpx;
  .btn { flex: 1; height: 96rpx; line-height: 96rpx; border-radius: 48rpx; font-size: 30rpx; }
  .btn-default { background: #F7F7F7; color: #333; }
  .btn-primary { background: #FF8866; color: #fff; }
}
.alt-entry { text-align: center; font-size: 26rpx; color: #FF8866; padding: 24rpx; }

.loading-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.loading-card {
  width: 560rpx; background: #fff; border-radius: 32rpx;
  padding: 48rpx 40rpx; text-align: center;
  .loading-icon { font-size: 96rpx; animation: bob 1.5s ease-in-out infinite; }
  .loading-title { font-size: 32rpx; font-weight: 600; margin-top: 16rpx; color: #333; }
  .loading-sub { font-size: 26rpx; color: #FF6644; margin-top: 12rpx; min-height: 36rpx; }
  .progress-bar { height: 12rpx; background: #f5f5f5; border-radius: 6rpx; overflow: hidden; margin: 24rpx 0 16rpx;
    .progress-inner { height: 100%; background: linear-gradient(90deg, #FF8866, #FFB199); transition: width .4s; }
  }
  .loading-time { font-size: 22rpx; color: #999; }
  .loading-tip { font-size: 22rpx; color: #ccc; margin-top: 16rpx; }
  .loading-cancel { font-size: 24rpx; color: #999; margin-top: 32rpx; padding: 16rpx; text-decoration: underline; }
}
@keyframes bob { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-12rpx)} }
</style>
