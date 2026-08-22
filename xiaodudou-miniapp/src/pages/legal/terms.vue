<template>
  <view class="page">
    <view class="header">
      <view class="title">用户协议</view>
      <view class="meta">修订日期：2026年8月21日 · 版本 v1.1</view>
    </view>

    <view class="section">
      <view class="p">欢迎使用小肚兜AI（以下简称"本应用"）。在使用本应用前，请您仔细阅读本协议。当您点击"同意"或继续使用，即视为您接受本协议全部条款。</view>
    </view>

    <view class="section">
      <view class="h2">1. 服务说明</view>
      <view class="p">本应用为母婴营养辅助工具，提供 AI 食材识别、菜谱推荐、营养分析、打卡记录等功能。</view>
      <view class="p"><text class="warn">⚠️ AI 输出会在结果区域以“AI辅助识别”或“AI辅助生成”标识，仅供参考，不可替代医生等专业人员意见。</text></view>
    </view>

    <view class="section">
      <view class="h2">2. 账号与登录</view>
      <view class="p">2.1 您通过微信授权登录，需对账号下所有行为负责。</view>
      <view class="p">2.2 请妥善保管账号，不得转让、出借给他人。</view>
      <view class="p">2.3 发现账号被盗用，请立即联系客服。</view>
    </view>

    <view class="section">
      <view class="h2">3. 用户行为规范</view>
      <view class="p">3.1 禁止上传违法违规、色情低俗、暴力血腥内容</view>
      <view class="p">3.2 禁止恶意刷接口、爬取数据、攻击系统</view>
      <view class="p">3.3 禁止冒充医生、营养师身份发布虚假指导内容</view>
      <view class="p">3.4 妈妈圈当前未开放，应用不会公开展示用户阶段画像或打卡动态</view>
      <view class="p">3.5 违反上述规定时，我们可依法采取限制使用等必要措施</view>
    </view>

    <view class="section">
      <view class="h2">4. 商业化功能</view>
      <view class="p">4.1 当前版本未开放会员购买、订单、支付或退款功能。</view>
      <view class="p">4.2 后续如开放，将在真实支付、退款和权益履约能力验收完成后另行展示规则，并在用户确认前明确说明。</view>
    </view>

    <view class="section">
      <view class="h2">5. 知识产权</view>
      <view class="p">5.1 本应用所有图片、文案、菜谱内容版权归本平台所有</view>
      <view class="p">5.2 当前版本不提供面向其他用户的内容发布或妈妈圈展示能力</view>
    </view>

    <view class="section">
      <view class="h2">6. 免责声明</view>
      <view class="p"><text class="warn">⚠️ 重要：本应用不对以下情况负责</text></view>
      <view class="p">6.1 因您未提供准确的过敏源、阶段信息导致的健康风险</view>
      <view class="p">6.2 您未咨询医生而采纳 AI 建议导致的任何后果</view>
      <view class="p">6.3 第三方 AI 服务异常或网络故障导致的服务中断</view>
      <view class="p">6.4 不可抗力（自然灾害、网络中断等）</view>
    </view>

    <view class="section">
      <view class="h2">7. 服务变更与终止</view>
      <view class="p">7.1 我们可不定期更新功能、调整界面，恕不另行通知</view>
      <view class="p">7.2 您可随时通过“我的-设置”注销账号；画像、收藏、打卡、反馈和 AI 日志将删除，账号身份字段将匿名化，已支付或已退款财务记录按必要规则保留</view>
      <view class="p">7.3 服务规则发生重要变化时，将按适用法律要求进行提示或公告</view>
    </view>

    <view class="section">
      <view class="h2">8. 争议解决</view>
      <view class="p">本协议适用中华人民共和国法律。如有争议，先友好协商；协商不成提交本应用运营公司所在地有管辖权的法院。</view>
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
.section { padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f5;
  &:last-of-type { border-bottom: none; }
  .h2 { font-size: 28rpx; font-weight: 600; color: #FF6644; margin-bottom: 16rpx; }
  .p { font-size: 26rpx; color: #444; line-height: 1.8; padding: 8rpx 0;
    .b { font-weight: 600; color: #333; }
    .warn { color: #FF6644; }
  }
}
.footer { text-align: center; color: #ccc; font-size: 22rpx; padding-top: 32rpx; }
</style>
