<template>
  <view class="page">
    <view class="header">
      <view class="title">用户协议</view>
      <view class="meta">生效日期：2026年5月24日 · 版本 v1.0</view>
    </view>

    <view class="section">
      <view class="p">欢迎使用小肚兜AI（以下简称"本应用"）。在使用本应用前，请您仔细阅读本协议。当您点击"同意"或继续使用，即视为您接受本协议全部条款。</view>
    </view>

    <view class="section">
      <view class="h2">1. 服务说明</view>
      <view class="p">本应用为母婴营养辅助工具，提供 AI 食材识别、菜谱推荐、营养分析、打卡记录等功能。</view>
      <view class="p"><text class="warn">⚠️ 本应用 AI 推荐内容仅为参考建议，不可替代医生、营养师的专业意见。</text></view>
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
      <view class="p">3.4 妈妈圈打卡内容应真实，禁止虚假摆拍</view>
      <view class="p">3.5 违反上述规定我们有权封禁账号且不予退款</view>
    </view>

    <view class="section">
      <view class="h2">4. 会员服务</view>
      <view class="p">4.1 您可购买月子卡、辅食年卡、儿童年卡等会员服务</view>
      <view class="p">4.2 会员权益包括但不限于 AI 调用次数提升、专家咨询次数等</view>
      <view class="p">4.3 <text class="b">虚拟服务一经开通即时生效，原则上不支持退款</text>；但首次购买的会员，可在 7 天内未使用任何付费功能的前提下申请全额退款</view>
      <view class="p">4.4 会员到期不自动续费，需手动续订</view>
    </view>

    <view class="section">
      <view class="h2">5. 知识产权</view>
      <view class="p">5.1 本应用所有图片、文案、菜谱内容版权归本平台所有</view>
      <view class="p">5.2 用户在妈妈圈上传的内容，授权本应用在小肚兜服务范围内使用（如脱敏后展示给其他妈妈）</view>
    </view>

    <view class="section">
      <view class="h2">6. 免责声明</view>
      <view class="p"><text class="warn">⚠️ 重要：本应用不对以下情况负责</text></view>
      <view class="p">6.1 因您未提供准确的过敏源、阶段信息导致的健康风险</view>
      <view class="p">6.2 您未咨询医生而采纳 AI 建议导致的任何后果</view>
      <view class="p">6.3 第三方（智谱 AI、微信支付）服务异常导致的损失</view>
      <view class="p">6.4 不可抗力（自然灾害、网络中断等）</view>
    </view>

    <view class="section">
      <view class="h2">7. 服务变更与终止</view>
      <view class="p">7.1 我们可不定期更新功能、调整界面，恕不另行通知</view>
      <view class="p">7.2 您可随时通过"我的-设置"注销账号</view>
      <view class="p">7.3 我们保留终止服务的权利，终止前 30 天会公告</view>
    </view>

    <view class="section">
      <view class="h2">8. 争议解决</view>
      <view class="p">本协议适用中华人民共和国法律。如有争议，先友好协商；协商不成提交本应用运营公司所在地有管辖权的法院。</view>
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
