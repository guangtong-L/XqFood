<template>
  <view class="page">
    <view v-if="loading && recipes.length === 0" class="loading">加载中...</view>

    <view v-else-if="errorMsg && recipes.length === 0" class="error-state">
      <view>收藏加载失败，无法判断是否为空</view>
      <button @tap="load(true)">重试</button>
    </view>

    <view v-else-if="recipes.length === 0" class="empty">
      <view class="empty-emoji">⭐</view>
      <view class="empty-title">还没有收藏</view>
      <view class="empty-sub">在菜谱详情页点 ❤ 收藏喜欢的菜</view>
      <button class="btn-go" @tap="goRecipes">去逛逛菜谱</button>
    </view>

    <template v-else>
      <view v-if="errorMsg" class="refresh-warning">
        <text>{{ errorMsg }}</text>
        <button @tap="load(true)">重新刷新</button>
      </view>
      <view class="header">共 {{ total }} 道收藏</view>
      <view class="list">
        <view v-for="r in recipes" :key="r.id" class="card" @tap="goDetail(r.id)">
          <image v-if="r.coverUrl && !failedImages.has(String(r.id))" class="cover" :src="r.coverUrl" mode="aspectFill" @error="markImageFailed(r.id)" />
          <view v-else class="cover image-fallback">🍲</view>
          <view class="info">
            <view class="title">{{ r.title }}</view>
            <view class="meta">
              <text v-if="r.cookMinutes">🕐 {{ r.cookMinutes }} 分钟</text>
              <text v-if="r.nutrition?.calories">🔥 {{ r.nutrition.calories }} kcal</text>
            </view>
            <view class="tags" v-if="r.stageTags && r.stageTags.length">
              <text v-for="t in r.stageTags.slice(0, 2)" :key="t" class="tag">{{ stageLabel(t) }}</text>
            </view>
          </view>
          <view class="unfav" :class="{ disabled: pendingIds.has(String(r.id)) }" @tap.stop="unfavorite(r.id)">{{ pendingIds.has(String(r.id)) ? '处理中' : '取消' }}</view>
        </view>
      </view>
      <button v-if="recipes.length < total" class="btn-more" :disabled="loading" @tap="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { favoriteApi } from '../../api/favorite'
import { feedback } from '../../utils/feedback'
import type { Recipe } from '../../api/recipe'

const STAGE_LABEL: Record<string, string> = {
  postpartum_early: '月子早期', postpartum_late: '月子后期',
  lactation: '哺乳期', weaning: '辅食', child: '儿童'
}
const stageLabel = (t: string) => STAGE_LABEL[t] || t

const recipes = ref<Recipe[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const errorMsg = ref('')
const failedImages = ref(new Set<string>())
const pendingIds = ref(new Set<string>())
const PAGE_SIZE = 20

async function load(reset = true) {
  if (loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const targetPage = reset ? 1 : page.value + 1
    const result = await favoriteApi.myList(targetPage, PAGE_SIZE)
    recipes.value = reset ? result.records : [...recipes.value, ...result.records]
    page.value = result.page
    total.value = result.total
  } catch (e) {
    errorMsg.value = recipes.value.length > 0
      ? (reset ? '刷新失败，当前为上次结果' : '加载更多失败，当前列表未更新')
      : (e instanceof Error ? e.message : '收藏加载失败，请检查网络')
  } finally {
    loading.value = false
  }
}

function loadMore() { load(false) }

async function unfavorite(id: string) {
  const key = String(id)
  if (pendingIds.value.has(key)) return
  const ok = await feedback.confirm('确认取消收藏？')
  if (!ok) return
  pendingIds.value = new Set(pendingIds.value).add(key)
  try {
    await favoriteApi.remove(id)
    recipes.value = recipes.value.filter(r => String(r.id) !== String(id))
    total.value = Math.max(0, total.value - 1)
    feedback.success('已取消')
  } catch (e) {
    feedback.error(e instanceof Error ? e.message : '取消失败，请重试')
  } finally {
    const next = new Set(pendingIds.value); next.delete(key); pendingIds.value = next
  }
}

function markImageFailed(id: string | number) { failedImages.value = new Set(failedImages.value).add(String(id)) }

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}
function goRecipes() {
  uni.switchTab({ url: '/pages/recipe/list' })
}

onShow(() => load(true))
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; }
.loading { padding: 160rpx 0; text-align: center; color: #999; font-size: 26rpx; }
.error-state { padding: 160rpx 32rpx; text-align: center; color: #777; font-size: 26rpx;
  button { margin-top: 28rpx; width: 220rpx; min-height: 80rpx; border-radius: 40rpx; background: #FFF0EB; color: #D94F2B; }
}
.refresh-warning { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-bottom: 16rpx; padding: 18rpx 22rpx; border-radius: 12rpx; background: #FFF4E5; color: #8A5700; font-size: 23rpx;
  text { flex: 1; }
  button { flex: none; margin: 0; min-width: 150rpx; min-height: 72rpx; line-height: 72rpx; padding: 0 20rpx; border-radius: 36rpx; background: #fff; color: #8A5700; font-size: 23rpx; }
}

.empty { padding: 160rpx 32rpx 32rpx; text-align: center;
  .empty-emoji { font-size: 160rpx; opacity: 0.5; }
  .empty-title { font-size: 32rpx; font-weight: 600; margin-top: 32rpx; }
  .empty-sub { font-size: 26rpx; color: #999; margin-top: 12rpx; }
  .btn-go { background: #FF8866; color: #fff; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; margin-top: 48rpx; width: 60%; }
}

.header { color: #999; font-size: 24rpx; padding: 16rpx; }
.btn-more { margin: 20rpx auto; width: 50%; color: #666; background: #fff; font-size: 25rpx; }

.list {
  .card { background: #fff; border-radius: 16rpx; overflow: hidden; margin-bottom: 24rpx; display: flex; position: relative;
    .cover { width: 200rpx; height: 200rpx; background: #f0f0f0; }
    .image-fallback { display: flex; align-items: center; justify-content: center; font-size: 56rpx; color: #aaa; flex-shrink: 0; }
    .info { flex: 1; padding: 24rpx;
      .title { font-size: 30rpx; font-weight: 500; }
      .meta { display: flex; gap: 16rpx; font-size: 22rpx; color: #999; margin-top: 12rpx; }
      .tags { display: flex; gap: 8rpx; margin-top: 12rpx; flex-wrap: wrap;
        .tag { font-size: 20rpx; color: #FF6644; background: #FFE5DC; padding: 4rpx 12rpx; border-radius: 8rpx; }
      }
    }
    .unfav {
      position: absolute; top: 16rpx; right: 16rpx;
      font-size: 22rpx; color: #999;
      padding: 8rpx 16rpx; border: 1rpx solid #e0e0e0;
      border-radius: 20rpx;
      min-width: 72rpx; min-height: 48rpx; line-height: 48rpx; text-align: center;
      &.disabled { opacity: .5; }
    }
  }
}
</style>
