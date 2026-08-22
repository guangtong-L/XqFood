<template>
  <view class="page">
    <view class="header">
      <view class="title">隐私政策</view>
      <view class="meta">修订日期：2026年8月21日 · 版本 v1.1</view>
    </view>

    <view class="section">
      <view class="h2">引言</view>
      <view class="p">小肚兜AI（以下简称"我们"）非常重视用户隐私和个人信息保护。本政策说明我们如何收集、使用、存储和保护您的个人信息。请您仔细阅读并理解本政策。</view>
    </view>

    <view class="section">
      <view class="h2">1. 我们收集的信息</view>
      <view class="p"><text class="b">1.1 账号信息</text>：通过微信登录获取的 openid、unionid、昵称、头像。</view>
      <view class="p"><text class="b">1.2 阶段画像</text>：您主动填写的孕周、产后天数、分娩方式、喂养方式、过敏源、忌口偏好。<text class="warn">⚠️ 这些为敏感个人信息，您可随时修改或删除。</text></view>
      <view class="p"><text class="b">1.3 使用数据</text>：收藏、打卡，以及不包含原始请求、食材名称、健康画像或完整输出的 AI 最小运行日志。</view>
      <view class="p"><text class="b">1.4 图片信息</text>：您主动上传的食材图片用于 AI 识别。当前服务端代码不主动将原图写入本地文件存储；第三方 AI 服务的处理规则以其公开政策和正式接入协议为准。</view>
    </view>

    <view class="section">
      <view class="h2">2. 信息使用目的</view>
      <view class="p">2.1 保存并展示您主动填写的母婴阶段资料，支持菜谱浏览、收藏和已食用记录</view>
      <view class="p">2.2 AI 识别与推荐当前未开放；完成内容安全与合规验收后如需开放，将按届时功能和政策另行说明用途</view>
      <view class="p">2.3 妈妈圈当前未开放，不会读取或展示头像、孕周、产后天数或打卡动态</view>
      <view class="p">2.4 改善服务质量、修复 Bug</view>
      <view class="p">2.5 AI 最小运行日志按生产配置定期清理，配置范围为 1 至 365 天；账号注销时立即删除该账号的 AI 日志。</view>
    </view>

    <view class="section">
      <view class="h2">3. 信息安全</view>
      <view class="p">我们采用 HTTPS 传输、最小权限访问等措施；母婴阶段、过敏、忌口和健康备注以带完整性校验的密文保存。</view>
      <view class="p"><text class="warn">⚠️ 我们将通过访问控制、传输保护和必要的审计措施保护信息；正式上线前仍需完成安全验收。</text></view>
    </view>

    <view class="section">
      <view class="h2">4. 您的权利</view>
      <view class="p">4.1 <text class="b">访问</text>：在"我的-档案"中查看您填写的所有信息</view>
      <view class="p">4.2 <text class="b">更正</text>：随时修改阶段画像</view>
      <view class="p">4.3 <text class="b">注销</text>：在“我的-设置”完成二次确认后，账号立即匿名化，并删除画像、收藏、打卡、反馈和 AI 日志</view>
      <view class="p">4.4 <text class="b">财务记录</text>：未支付、已取消或已过期订单随注销删除；已支付或已退款记录仅为财务合规保留，账号身份信息已匿名化</view>
      <view class="p">4.5 <text class="b">其他数据请求</text>：可通过“帮助与反馈”提出</view>
      <view class="p">4.6 <text class="b">撤回授权</text>：在微信"设置-隐私"撤销小程序授权</view>
    </view>

    <view class="section">
      <view class="h2">5. 第三方共享</view>
      <view class="p">5.1 智谱 AI：识别时传输您主动上传的食材图片；推荐时传输您主动提交的阶段、食材和限制条件，不传输账号身份字段</view>
      <view class="p">5.2 当前版本未开放会员购买、支付和退款，不会因这些功能向支付机构传输订单信息。</view>
      <view class="p">5.3 我们承诺不向任何第三方出售您的个人信息。</view>
    </view>

    <view class="section">
      <view class="h2">6. 未成年人保护</view>
      <view class="p">本应用主要服务对象为成年妈妈。若您为未成年人或涉及未成年人健康数据，请在家长监护下使用并仔细阅读本政策。</view>
    </view>

    <view class="section">
      <view class="h2">7. AI 推荐免责声明</view>
      <view class="p"><text class="warn">⚠️ AI 识别和推荐结果会在结果区域以“AI辅助识别”或“AI辅助生成”标识，仅供参考，不构成医疗建议或诊断。特殊体质、过敏体质、孕产期病史等情况请咨询专业医生。</text></view>
    </view>

    <view class="section">
      <view class="h2">8. 政策更新</view>
      <view class="p">本政策可能不定期更新，更新时我们会通过应用内推送通知您，请及时查看。</view>
    </view>

    <view class="section">
      <view class="h2">9. 联系我们</view>
      <view class="p">如需提出问题，请使用应用内“帮助与反馈”。正式运营主体及有效联系方式将在确认后公示。</view>
    </view>

    <view class="footer">最后修订：2026-08-21</view>
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
