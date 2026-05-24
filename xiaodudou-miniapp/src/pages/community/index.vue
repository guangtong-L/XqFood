<template>
  <view class="page">
    <view class="header">
      <view class="header-top">
        <text class="title">妈妈圈 · {{ stageLabel }}</text>
        <text class="active">🔥 {{ activeUsers }} 位活跃</text>
      </view>
      <view class="header-sub">看看同阶段妈妈在吃什么</view>
    </view>

    <view v-if="loading && records.length === 0" class="loading">加载中...</view>

    <view v-else-if="records.length === 0" class="empty">
      <view class="empty-emoji">💬</view>
      <view class="empty-title">最近 24 小时还没人打卡</view>
      <view class="empty-sub">你来做第一个吧</view>
      <button class="btn-go" @tap="goRecipes">去选菜打卡</button>
    </view>

    <template v-else>
      <view class="feed-list">
        <view v-for="item in records" :key="item.actionId" class="feed-card" @tap="goDetail(item.recipeId)">
          <view class="user-row">
            <image v-if="item.avatarUrl" :src="item.avatarUrl" class="avatar" />
            <view v-else class="avatar emoji-avatar">👩</view>
            <view class="user-info">
              <view class="name">{{ item.displayName }}</view>
              <view class="stage">{{ item.stageDesc }} · {{ formatTime(item.checkedAt) }}</view>
            </view>
          </view>
          <view class="recipe-row">
            <image v-if="item.recipeCover" :src="item.recipeCover" class="recipe-img" mode="aspectFill" />
            <view v-else class="recipe-img-placeholder">🍽</view>
            <view class="recipe-info">
              <view class="recipe-title">「{{ item.recipeTitle }}」</view>
              <view class="recipe-action">✓ 已打卡</view>
            </view>
          </view>
        </view>
      </view>

      <view class="footer-tip">显示最近 24 小时的打卡</view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { communityApi, type FeedItem } from '../../api/community'

const records = ref<FeedItem[]>([])
const activeUsers = ref(0)
const stageLabel = ref('哺乳期')
const loading = ref(false)

const STAGE_LABEL: Record<string, string> = {
  PREPARE: '备孕', PREGNANCY: '孕期', POSTPARTUM: '哺乳期',
  WEANING: '辅食期', CHILD: '儿童期'
}

async function load() {
  loading.value = true
  try {
    const data = await communityApi.feed({ page: 1, size: 30 })
    records.value = data.records || []
    activeUsers.value = data.activeUsers || 0
    stageLabel.value = STAGE_LABEL[data.stageType] || data.stageType || '通用'
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function formatTime(s: string) {
  if (!s) return ''
  const now = new Date()
  const t = new Date(s.replace(' ', 'T'))
  const diff = Math.floor((now.getTime() - t.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return Math.floor(diff / 86400) + ' 天前'
}

function goDetail(id: string) { uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` }) }
function goRecipes() { uni.switchTab({ url: '/pages/recipe/list' }) }

onShow(load)
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; }

.header { background: linear-gradient(135deg, #FF8866, #FFB199); border-radius: 24rpx; padding: 32rpx; color: #fff; margin-bottom: 24rpx;
  .header-top { display: flex; justify-content: space-between; align-items: center;
    .title { font-size: 32rpx; font-weight: 600; }
    .active { font-size: 24rpx; background: rgba(255,255,255,0.25); padding: 8rpx 16rpx; border-radius: 24rpx; }
  }
  .header-sub { font-size: 24rpx; margin-top: 12rpx; opacity: 0.9; }
}

.loading { text-align: center; color: #999; padding: 160rpx 0; font-size: 26rpx; }

.empty { padding: 120rpx 32rpx 32rpx; text-align: center;
  .empty-emoji { font-size: 160rpx; opacity: 0.5; }
  .empty-title { font-size: 32rpx; font-weight: 600; margin-top: 32rpx; }
  .empty-sub { font-size: 26rpx; color: #999; margin-top: 12rpx; }
  .btn-go { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; margin-top: 48rpx; width: 60%; }
}

.feed-list .feed-card {
  background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 24rpx;
  .user-row { display: flex; align-items: center; margin-bottom: 20rpx;
    .avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #FFE5DC;
      &.emoji-avatar { text-align: center; line-height: 72rpx; font-size: 40rpx; }
    }
    .user-info { flex: 1; margin-left: 16rpx;
      .name { font-size: 28rpx; font-weight: 500; }
      .stage { font-size: 22rpx; color: #999; margin-top: 4rpx; }
    }
  }
  .recipe-row { display: flex; background: #FFF8F5; border-radius: 16rpx; overflow: hidden; padding: 16rpx;
    .recipe-img { width: 120rpx; height: 120rpx; border-radius: 12rpx; background: #f0f0f0; }
    .recipe-img-placeholder { width: 120rpx; height: 120rpx; line-height: 120rpx; text-align: center; font-size: 56rpx; background: #fff; border-radius: 12rpx; }
    .recipe-info { flex: 1; padding-left: 16rpx; display: flex; flex-direction: column; justify-content: center;
      .recipe-title { font-size: 28rpx; font-weight: 500; }
      .recipe-action { font-size: 22rpx; color: #4CAF50; margin-top: 8rpx; }
    }
  }
}

.footer-tip { text-align: center; color: #ccc; font-size: 22rpx; padding: 24rpx; }
</style>
