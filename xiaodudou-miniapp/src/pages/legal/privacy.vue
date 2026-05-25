<template>
  <view class="page">
    <view class="header">
      <view class="title">隐私政策</view>
      <view class="meta">生效日期：2026年5月24日 · 版本 v1.0</view>
    </view>

    <view class="section">
      <view class="h2">引言</view>
      <view class="p">小肚兜AI（以下简称"我们"）非常重视用户隐私和个人信息保护。本政策说明我们如何收集、使用、存储和保护您的个人信息。请您仔细阅读并理解本政策。</view>
    </view>

    <view class="section">
      <view class="h2">1. 我们收集的信息</view>
      <view class="p"><text class="b">1.1 账号信息</text>：通过微信登录获取的 openid、unionid、昵称、头像。</view>
      <view class="p"><text class="b">1.2 阶段画像</text>：您主动填写的孕周、产后天数、分娩方式、喂养方式、过敏源、忌口偏好。<text class="warn">⚠️ 这些为敏感个人信息，您可随时修改或删除。</text></view>
      <view class="p"><text class="b">1.3 使用数据</text>：菜谱浏览、收藏、打卡、AI 调用记录。</view>
      <view class="p"><text class="b">1.4 图片信息</text>：您主动上传的食材图片仅用于 AI 识别，识别完成后 24 小时内自动删除原图。</view>
    </view>

    <view class="section">
      <view class="h2">2. 信息使用目的</view>
      <view class="p">2.1 提供个性化菜谱推荐和营养分析</view>
      <view class="p">2.2 识别食材并生成做法</view>
      <view class="p">2.3 同阶段妈妈圈匿名打卡聚合（昵称脱敏处理）</view>
      <view class="p">2.4 改善服务质量、修复 Bug</view>
      <view class="p">2.5 法律法规要求的合规留存（AI 调用日志保留 180 天）</view>
    </view>

    <view class="section">
      <view class="h2">3. 信息安全</view>
      <view class="p">我们采用 HTTPS 传输、加密存储、最小权限访问等技术措施保护您的个人信息。</view>
      <view class="p"><text class="warn">⚠️ 您的健康数据采用 AES-256 加密存储，员工无法直接查阅。</text></view>
    </view>

    <view class="section">
      <view class="h2">4. 您的权利</view>
      <view class="p">4.1 <text class="b">访问</text>：在"我的-档案"中查看您填写的所有信息</view>
      <view class="p">4.2 <text class="b">更正</text>：随时修改阶段画像</view>
      <view class="p">4.3 <text class="b">删除</text>：在"我的-设置"中注销账号（数据 30 天内删除）</view>
      <view class="p">4.4 <text class="b">导出</text>：联系客服可获取您的全部数据副本</view>
      <view class="p">4.5 <text class="b">撤回授权</text>：在微信"设置-隐私"撤销小程序授权</view>
    </view>

    <view class="section">
      <view class="h2">5. 第三方共享</view>
      <view class="p">5.1 智谱 AI：仅传输您拍摄的食材图片用于识别（不含身份信息）</view>
      <view class="p">5.2 微信支付：仅在您下单时传输订单金额（不含敏感信息）</view>
      <view class="p">5.3 我们承诺不向任何第三方出售您的个人信息。</view>
    </view>

    <view class="section">
      <view class="h2">6. 未成年人保护</view>
      <view class="p">本应用主要服务对象为成年妈妈。若您为未成年人或涉及未成年人健康数据，请在家长监护下使用并仔细阅读本政策。</view>
    </view>

    <view class="section">
      <view class="h2">7. AI 推荐免责声明</view>
      <view class="p"><text class="warn">⚠️ 本应用所有 AI 推荐、营养分析仅供参考，不构成医疗建议或诊断。特殊体质、过敏体质、孕产期病史等情况请咨询专业医生或营养师。</text></view>
    </view>

    <view class="section">
      <view class="h2">8. 政策更新</view>
      <view class="p">本政策可能不定期更新，更新时我们会通过应用内推送通知您，请及时查看。</view>
    </view>

    <view class="section">
      <view class="h2">9. 联系我们</view>
      <view class="p">客服邮箱：contact@xiaodudou.ai</view>
      <view class="p">投诉热线：400-XXX-XXXX（工作日 9:00-18:00）</view>
    </view>

    <view class="footer">最后修订：2026-05-24</view>
  </view>
</template>

<script setup lang="ts"></script>

<style lang="scss" scoped>
.page { padding: 32rpx; background: #fff; min-height: 100vh; padding-bottom: 80rpx; }
.header { padding-bottom: 32rpx; border-bottom: 1rpx solid #f0f0f0;
  .title { font-size: 40rpx; font-weight: 600; }
  .meta { font-size: 22rpx; color: #999; margin-top: 12rpx; }
}
.section { padding: 32rpx 0; border-bottom: 1rpx solid #f5f5f5;
  &:last-of-type { border-bottom: none; }
  .h2 { font-size: 28rpx; font-weight: 600; color: #FF6644; margin-bottom: 16rpx; }
  .p { font-size: 26rpx; color: #444; line-height: 1.8; padding: 8rpx 0;
    .b { font-weight: 600; color: #333; }
    .warn { color: #FF6644; }
  }
}
.footer { text-align: center; color: #ccc; font-size: 22rpx; padding-top: 32rpx; }
</style>
