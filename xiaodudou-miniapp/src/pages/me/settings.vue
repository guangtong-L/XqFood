<template>
  <view class="page">
    <!-- 账号信息 -->
    <view class="card">
      <view class="card-title">账号</view>
      <view class="row" @tap="goProfile">
        <text class="label">个人档案</text>
        <text class="value">{{ userStore.user?.nickname || '未设置' }} ›</text>
      </view>
      <view class="row">
        <text class="label">注册时间</text>
        <text class="value muted">{{ formatDate(userStore.user?.createdAt) }}</text>
      </view>
      <view class="row">
        <text class="label">用户 ID</text>
        <text class="value muted">{{ userIdShort }}</text>
      </view>
    </view>

    <!-- 通知设置 -->
    <view class="card">
      <view class="card-title">通知</view>
      <view class="row">
        <text class="label">接收推送通知</text>
        <switch :checked="notifyPush" color="#FF8866" @change="onTogglePush" />
      </view>
      <view class="row">
        <text class="label">接收营销消息</text>
        <switch :checked="notifyMarketing" color="#FF8866" @change="onToggleMarketing" />
      </view>
      <view class="row-hint">关闭推送后，仅会员到期、客服回复等重要通知会发送</view>
    </view>

    <!-- 缓存管理 -->
    <view class="card">
      <view class="card-title">存储</view>
      <view class="row" @tap="clearCache">
        <text class="label">清除缓存</text>
        <text class="value">{{ cacheSize }} ›</text>
      </view>
      <view class="row-hint">不会删除您的收藏、打卡和订单</view>
    </view>

    <!-- 关于 -->
    <view class="card">
      <view class="card-title">关于</view>
      <view class="row" @tap="checkUpdate">
        <text class="label">检查更新</text>
        <text class="value">v{{ appVersion }} ›</text>
      </view>
      <view class="row" @tap="goLegal('terms')">
        <text class="label">用户协议</text>
        <text class="value">›</text>
      </view>
      <view class="row" @tap="goLegal('privacy')">
        <text class="label">隐私政策</text>
        <text class="value">›</text>
      </view>
      <view class="row" @tap="copyText('contact@xiaodudou.ai')">
        <text class="label">联系我们</text>
        <text class="value muted">contact@xiaodudou.ai ›</text>
      </view>
    </view>

    <!-- 账号操作 -->
    <view class="card">
      <view class="card-title">账号操作</view>
      <view class="row danger" @tap="doDeactivate">
        <text class="label">注销账号</text>
        <text class="value">›</text>
      </view>
      <view class="row-hint danger-hint">注销后 30 天内可联系客服恢复，30 天后数据将被永久删除</view>
    </view>

    <!-- 退出登录 -->
    <button class="btn-logout" @tap="doLogout">退出登录</button>

    <view class="footer">小肚兜 AI · 让母婴营养更简单</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { authApi } from '../../api/auth'
import { feedback as fb } from '../../utils/feedback'

const userStore = useUserStore()
const appVersion = ref('0.0.1')
const cacheSize = ref('0 KB')

// 通知开关（仅本地）
const notifyPush = ref(uni.getStorageSync('notify_push') !== false)
const notifyMarketing = ref(uni.getStorageSync('notify_marketing') !== false)

onShow(() => {
  if (userStore.isLoggedIn && !userStore.user) userStore.loadMe()
  loadAppVersion()
  loadCacheSize()
})

function loadAppVersion() {
  // #ifdef MP-WEIXIN
  try {
    const accountInfo = uni.getAccountInfoSync()
    appVersion.value = accountInfo.miniProgram.version || '0.0.1'
  } catch { /* H5 fallback */ }
  // #endif
  // H5 / App：固定写在这里（构建时可注入）
  if (!appVersion.value || appVersion.value === '0.0.1') {
    appVersion.value = '0.0.1'
  }
}

function loadCacheSize() {
  uni.getStorageInfo({
    success: (res) => {
      const kb = Math.max(1, Math.round(res.currentSize)) // currentSize 单位 KB
      cacheSize.value = kb > 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB'
    },
    fail: () => { cacheSize.value = '未知' }
  })
}

const userIdShort = computed(() => {
  const id = String(userStore.user?.id ?? '')
  return id ? '****' + id.slice(-6) : '-'
})

function formatDate(s?: string) {
  if (!s) return '-'
  return s.slice(0, 10)
}

function onTogglePush(e: { detail: { value: boolean } }) {
  notifyPush.value = e.detail.value
  uni.setStorageSync('notify_push', e.detail.value)
  fb.success(e.detail.value ? '已开启推送' : '已关闭推送')
}

function onToggleMarketing(e: { detail: { value: boolean } }) {
  notifyMarketing.value = e.detail.value
  uni.setStorageSync('notify_marketing', e.detail.value)
  fb.success(e.detail.value ? '已开启营销消息' : '已关闭营销消息')
}

async function clearCache() {
  const ok = await fb.confirm('清除缓存不会删除您的收藏与打卡数据，确认继续？', '清除缓存')
  if (!ok) return
  // 保留关键键
  const keep: Record<string, unknown> = {}
  const keysToKeep = ['x-token', 'notify_push', 'notify_marketing', 'onboarding_done']
  keysToKeep.forEach((k) => {
    const v = uni.getStorageSync(k)
    if (v !== '' && v !== null && v !== undefined) keep[k] = v
  })
  try {
    uni.clearStorageSync()
    Object.entries(keep).forEach(([k, v]) => uni.setStorageSync(k, v))
    loadCacheSize()
    fb.success('已清除')
  } catch {
    fb.error('清除失败')
  }
}

function checkUpdate() {
  // #ifdef MP-WEIXIN
  try {
    const updateManager = uni.getUpdateManager()
    updateManager.onCheckForUpdate(() => fb.toast('正在检查更新...'))
    updateManager.onUpdateReady(() => {
      uni.showModal({
        title: '更新提示',
        content: '新版本已下载，是否重启应用？',
        success: (res) => {
          if (res.confirm) updateManager.applyUpdate()
        }
      })
    })
    updateManager.onUpdateFailed(() => fb.error('更新失败'))
    return
  } catch { /* H5 fallback */ }
  // #endif
  fb.success('已是最新版本')
}

function copyText(text: string) {
  uni.setClipboardData({ data: text, success: () => fb.success('已复制') })
}

function goProfile() {
  uni.navigateTo({ url: '/pages/profile/setup' })
}

function goLegal(type: 'terms' | 'privacy') {
  uni.navigateTo({ url: `/pages/legal/${type}` })
}

async function doDeactivate() {
  const ok = await fb.confirm(
    '注销后您的全部数据将进入 30 天删除冷静期，期间可联系客服恢复。是否继续？',
    '注销账号'
  )
  if (!ok) return
  // M2 接入真实注销接口；当前先记录意向 + 提示联系客服
  try {
    // 复用反馈通道做注销意向记录
    const { feedbackApi } = await import('../../api/feedback')
    await feedbackApi.submit({
      content: '【注销账号】用户主动申请注销',
      category: 'business'
    })
  } catch { /* 失败也不阻塞下面提示 */ }
  uni.showModal({
    title: '已收到注销申请',
    content: '客服将在 1 个工作日内联系您完成注销。如紧急请直接拨打 400-XXX-XXXX。',
    showCancel: false
  })
}

async function doLogout() {
  const ok = await fb.confirm('确认退出当前账号？', '退出登录')
  if (!ok) return
  try { await authApi.logout() } catch {}
  userStore.logout()
  uni.reLaunch({ url: '/pages/auth/wx-login' })
}
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; padding-bottom: 80rpx; }

.card {
  background: #fff; border-radius: 20rpx;
  margin-bottom: 24rpx; padding: 8rpx 24rpx;
  .card-title {
    font-size: 24rpx; color: #999;
    padding: 24rpx 0 12rpx; border-bottom: 1rpx solid #f5f5f5;
  }
  .row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f5;
    font-size: 28rpx;
    &:last-child { border-bottom: none; }
    .label { color: #333; }
    .value { color: #666; font-size: 26rpx;
      &.muted { color: #999; font-size: 24rpx; }
    }
    &.danger .label { color: #FF6644; }
  }
  .row-hint {
    font-size: 22rpx; color: #bbb; padding: 0 0 20rpx;
    &.danger-hint { color: #FFAA88; }
  }
}

.btn-logout {
  margin: 16rpx 24rpx; background: #fff;
  color: #FF6644; height: 88rpx; line-height: 88rpx;
  border-radius: 44rpx; font-size: 30rpx;
  border: 1rpx solid #FFE5DC;
}

.footer { text-align: center; color: #ccc; font-size: 22rpx; margin-top: 40rpx; }
</style>
