<template>
  <view class="page">
    <view class="hero">
      <view class="emoji">💁‍♀️</view>
      <view class="title">在线客服</view>
      <view class="sub">工作日 9:00-21:00，30 分钟内响应</view>
    </view>

    <view class="contact-card">
      <view class="contact-row" @tap="copy('contact@xiaodudou.ai')">
        <text class="icon">📧</text>
        <view class="info">
          <view class="label">客服邮箱</view>
          <view class="value">contact@xiaodudou.ai</view>
        </view>
        <text class="action">复制</text>
      </view>
      <view class="contact-row" @tap="copy('xddai_service')">
        <text class="icon">💬</text>
        <view class="info">
          <view class="label">企业微信客服</view>
          <view class="value">xddai_service</view>
        </view>
        <text class="action">复制</text>
      </view>
      <view class="contact-row" @tap="copy('400-XXX-XXXX')">
        <text class="icon">📞</text>
        <view class="info">
          <view class="label">客服热线</view>
          <view class="value">400-XXX-XXXX</view>
        </view>
        <text class="action">复制</text>
      </view>
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
  { q: 'AI 识别不准确怎么办？', a: '可以点击识别结果页的 + 手动添加，或者长按食材删除。我们也建议把冰箱内食材尽量平铺，避免重叠和反光。', open: false },
  { q: '今日免费额度用完了？', a: '免费用户每日 5 次 AI 调用。开通月子卡（599元）即可享 50 次/日 + 营养师咨询权益。', open: false },
  { q: '过敏源会被严格过滤吗？', a: '是的。我们在召回和 AI 推荐两个环节都做硬规则过滤，含过敏源食材的菜谱不会出现在推荐中。但您仍需在食用前自行确认。', open: false },
  { q: '可以修改阶段画像吗？', a: '可以。在"我的-档案"中随时修改孕周、产后天数、过敏源等信息。', open: false },
  { q: '会员可以退款吗？', a: '首次购买可在 7 天内未使用任何付费功能的前提下申请全额退款。已大量使用付费功能后原则上不退款。', open: false },
  { q: '妈妈圈能看到我的真实昵称吗？', a: '不会。我们对所有妈妈圈展示的昵称做脱敏处理（如"B**"），并且您看不到自己的打卡，避免泄露身份。', open: false },
  { q: '如何注销账号？', a: '"我的-设置-注销账号"。提交后 30 天内我们会删除您的全部数据（依法律要求的部分日志保留 180 天）。', open: false }
])

function toggle(idx: number) { faqs.value[idx].open = !faqs.value[idx].open }

function copy(text: string) {
  uni.setClipboardData({
    data: text,
    success: () => fb.success('已复制')
  })
}

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
    fb.success('已提交，工作日 30 分钟内回复')
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
