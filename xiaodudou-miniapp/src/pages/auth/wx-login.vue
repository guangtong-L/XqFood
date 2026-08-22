<template>
  <view class="page">
    <view class="logo-area">
      <view class="logo">🤱</view>
      <view class="brand">小肚兜AI</view>
      <view class="slogan">记录、收藏并查看母婴阶段菜谱</view>
    </view>

    <view class="login-area">
      <button class="btn-wx" :disabled="loading" @tap="doLogin">
        <text v-if="!loading">微信一键登录</text>
        <text v-else>登录中...</text>
      </button>

      <view class="agree">
        <text class="check" @tap="agreed = !agreed">{{ agreed ? '☑' : '☐' }}</text>
        <text>我已阅读并同意</text>
        <text class="link" @tap.stop="openLegal('terms')">《用户协议》</text>
        <text>和</text>
        <text class="link" @tap.stop="openLegal('privacy')">《隐私政策》</text>
      </view>
    </view>

    <!-- #ifndef MP-WEIXIN -->
    <view class="dev-tip">当前平台登录能力以部署配置为准</view>
    <!-- #endif -->
    <!-- #ifdef MP-WEIXIN -->
    <view class="dev-tip">点击即调用微信授权登录</view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { authApi } from '../../api/auth'
import { useUserStore } from '../../store/user'
import { ApiError } from '../../utils/request'

const userStore = useUserStore()
const loading = ref(false)
const agreed = ref(false)

/**
 * 获取登录 code
 * - 微信小程序：调用 uni.login 拿到真实 code
 * - H5/其他：仅开发构建且显式开启 VITE_MOCK_LOGIN_ENABLED 时生成 Mock code
 */
function openLegal(type: 'terms' | 'privacy') {
  uni.navigateTo({ url: `/pages/legal/${type}` })
}

function getLoginCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.login({
      provider: 'weixin',
      success: (res) => {
        if (res.code) resolve(res.code)
        else reject(new Error('微信未返回登录凭证，请重试'))
      },
      fail: () => reject(new Error('微信登录失败，请检查网络后重试'))
    })
    // #endif
    // #ifndef MP-WEIXIN
    if (import.meta.env.DEV && import.meta.env.VITE_MOCK_LOGIN_ENABLED === 'true') {
      const stable = uni.getStorageSync('mock_code') || `dev_${Date.now()}`
      uni.setStorageSync('mock_code', stable)
      resolve(stable)
    } else {
      reject(new Error('当前平台暂不支持微信登录'))
    }
    // #endif
  })
}

async function doLogin() {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const code = await getLoginCode()

    const resp = await authApi.wxLogin({
      code,
      nickname: undefined,
      avatarUrl: undefined
    })

    userStore.setToken(resp.token)
    await userStore.loadMe()

    // 提示登录模式（H5/小程序未配 AppID 时是 mock）
    if (resp.loginMode === 'mock') {
      console.log('[Auth] 当前为 Mock 登录模式（后端未配置真实微信 AppID）')
    }

    // 已有阶段画像 → 进首页；否则 → 引导画像
    if (userStore.profile?.stageType) {
      uni.switchTab({ url: '/pages/index/index' })
    } else {
      uni.redirectTo({ url: '/pages/profile/setup' })
    }
  } catch (e) {
    if (!(e instanceof ApiError)) {
      uni.showToast({ title: e instanceof Error ? e.message : '登录失败，请稍后重试', icon: 'none' })
    }
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFE5DC 0%, #FFFFFF 60%);
  padding: 80rpx 48rpx;
  display: flex;
  flex-direction: column;
}

.logo-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .logo { font-size: 200rpx; }
  .brand { font-size: 56rpx; font-weight: bold; margin-top: 32rpx; color: #FF6644; }
  .slogan { font-size: 28rpx; color: #999; margin-top: 16rpx; }
}

.login-area {
  padding-bottom: 80rpx;

  .btn-wx {
    background: #FF8866;
    color: #fff;
    height: 96rpx;
    line-height: 96rpx;
    font-size: 32rpx;
    border-radius: 48rpx;
    box-shadow: 0 8rpx 24rpx rgba(255, 136, 102, 0.3);
    margin-bottom: 32rpx;
  }
  .btn-wx[disabled] { opacity: 0.6; }

  .agree {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24rpx;
    color: #999;
    flex-wrap: wrap;
    .check { font-size: 32rpx; margin-right: 8rpx; color: #FF8866; }
    .link { color: #FF8866; }
  }
}

.dev-tip {
  text-align: center;
  font-size: 22rpx;
  color: #ccc;
  padding: 24rpx 0;
}
</style>
