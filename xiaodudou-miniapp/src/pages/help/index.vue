<template>
  <view class="page">
    <view class="hero">
      <view class="emoji">💁‍♀️</view>
      <view class="title">帮助与反馈</view>
      <view class="sub">可在下方提交使用问题或改进建议</view>
    </view>

    <view class="section">
      <view class="section-title">🔥 常见问题</view>
      <view v-for="(item, idx) in faqs" :key="idx" class="faq-item" @tap="toggle(idx)">
        <view class="q">
          <text class="q-text">{{ item.q }}</text>
          <text class="q-arrow" :class="{ open: item.open }">▾</text>
        </view>
        <view v-if="item.open" class="a">{{ item.a }}</view>
      </view>
    </view>

    <view class="section">
      <view class="section-title">📝 意见反馈</view>
      <textarea class="feedback-input" v-model="feedback" placeholder="期待您的建议，让小肚兜变得更好" maxlength="500" />
      <button class="btn-submit" :disabled="!feedback.trim() || submitting" @tap="submitFeedback">
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
    </view>

    <view class="footer">
      <text @tap="goLegal('privacy')">隐私政策</text>
      <text class="dot">·</text>
      <text @tap="goLegal('terms')">用户协议</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { feedback as fb } from '../../utils/feedback'
import { feedbackApi } from '../../api/feedback'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

const feedback = ref('')
const submitting = ref(false)

const faqs = ref([
  { q: 'AI 识别与推荐现在可以使用吗？', a: '暂未开放。真实内容安全审核能力完成接入和验收前，生产环境不会提供 AI 识别与推荐；菜谱浏览、收藏和记录功能不受影响。', open: false },
  { q: '会员或额度可以购买吗？', a: '暂不可以。会员购买、支付和额度升级均未开放。', open: false },
  { q: '过敏标签能保证安全吗？', a: '不能。系统仅基于现有食材标签降低已知冲突风险，标签可能不完整，食用前仍需结合配料、包装和个人情况人工核对。', open: false },
  { q: '可以修改阶段画像吗？', a: '可以。在"我的-档案"中随时修改孕周、产后天数、过敏源等信息。', open: false },
  { q: '妈妈圈会展示我的资料吗？', a: '不会。妈妈圈当前未开放，也不会请求或展示用户头像、孕周、产后天数或打卡动态。', open: false },
  { q: '如何注销账号？', a: '请前往“我的-设置-永久注销账号”，阅读删除与财务记录保留规则后完成二次确认。', open: false }
])

function toggle(idx: number) { faqs.value[idx].open = !faqs.value[idx].open }

function goLegal(type: 'privacy' | 'terms') {
  uni.navigateTo({ url: `/pages/legal/${type}` })
}

async function submitFeedback() {
  const content = feedback.value.trim()
  if (!content) return
  if (content.length < 5) {
    fb.toast('反馈内容至少 5 个字')
    return
  }
  if (!userStore.isLoggedIn) {
    const ok = await fb.confirm('登录后可查看回复进度，是否前往登录？', '提示')
    if (ok) uni.navigateTo({ url: '/pages/auth/wx-login' })
    return
  }

  submitting.value = true
  try {
    // 采集端环境信息（不带敏感数据）
    const sysInfo = uni.getSystemInfoSync()
    const clientInfo: Record<string, unknown> = {
      platform: sysInfo.platform,
      system: sysInfo.system,
      model: sysInfo.model,
      version: sysInfo.version,
      appVersion: sysInfo.appVersion || '0.0.1'
    }
    await feedbackApi.submit({ content, category: 'general', clientInfo })
    fb.success('反馈已提交')
    feedback.value = ''
  } catch (e: unknown) {
    // 网络/服务异常 → 本地兜底，下次启动可重发（后续可扩展）
    const list = uni.getStorageSync('local_feedback_pending') || []
    list.push({ content, time: new Date().toISOString() })
    uni.setStorageSync('local_feedback_pending', list)
    // 已被 request 工具 toast 过，这里不重复 toast
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; background: #F7F7F7; min-height: 100vh; }

.hero { background: linear-gradient(135deg, #FF8866, #FFB199); border-radius: 24rpx; padding: 48rpx 32rpx; text-align: center; color: #fff; margin-bottom: 24rpx;
  .emoji { font-size: 96rpx; }
  .title { font-size: 32rpx; font-weight: 600; margin-top: 16rpx; }
  .sub { font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }
}

.contact-card { background: #fff; border-radius: 24rpx; padding: 16rpx 24rpx; margin-bottom: 24rpx;
  .contact-row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .icon { font-size: 40rpx; margin-right: 24rpx; }
    .info { flex: 1;
      .label { font-size: 22rpx; color: #999; }
      .value { font-size: 28rpx; color: #333; margin-top: 4rpx; }
    }
    .action { color: #FF8866; font-size: 24rpx; padding: 8rpx 16rpx; border: 1rpx solid #FFE5DC; border-radius: 24rpx; }
  }
}

.section { background: #fff; border-radius: 24rpx; padding: 32rpx 24rpx; margin-bottom: 24rpx;
  .section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; }
  .faq-item { padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .q { display: flex; justify-content: space-between; align-items: center;
      .q-text { font-size: 28rpx; color: #333; flex: 1; }
      .q-arrow { color: #999; transition: transform .2s;
        &.open { transform: rotate(180deg); }
      }
    }
    .a { font-size: 24rpx; color: #666; line-height: 1.6; margin-top: 12rpx; padding: 16rpx 0; background: #FFF8F5; border-radius: 8rpx; padding: 12rpx 16rpx; }
  }

  .feedback-input { width: 100%; min-height: 200rpx; padding: 24rpx; background: #F7F7F7; border-radius: 12rpx; font-size: 26rpx; box-sizing: border-box; }
  .btn-submit { margin-top: 24rpx; background: #FF8866; color: #fff; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; font-size: 28rpx;
    &[disabled] { opacity: 0.5; }
  }
}

.footer { text-align: center; padding: 32rpx; color: #999; font-size: 24rpx;
  .dot { padding: 0 16rpx; color: #ccc; }
}
</style>
