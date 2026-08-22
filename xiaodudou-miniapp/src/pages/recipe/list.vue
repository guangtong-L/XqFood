<template>
  <view class="page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" maxlength="50" placeholder="搜菜名（最多50字）" @confirm="reload" />
      <text class="search-btn" :class="{ disabled: loading }" @tap="reload">搜索</text>
    </view>

    <scroll-view scroll-x class="tabs" show-scrollbar="false">
      <view v-for="t in TABS" :key="t.value"
            class="tab" :class="{ active: stageTag === t.value }"
            @tap="switchTab(t.value)">
        {{ t.label }}
      </view>
    </scroll-view>

    <view class="list">
      <view v-if="loading && recipes.length === 0" class="empty">加载中...</view>
      <view v-else-if="errorMsg" class="state-error">{{ errorMsg }}<button @tap="reload">重试</button></view>
      <view v-else-if="recipes.length === 0" class="empty">暂无符合条件的上架菜谱</view>
      <view v-for="r in recipes" :key="r.id" class="card" @tap="goDetail(r.id)">
        <image v-if="r.coverUrl && !failedImages.has(String(r.id))" class="cover" :src="r.coverUrl" mode="aspectFill" @error="markImageFailed(r.id)" />
        <view v-else class="cover image-fallback">🍲</view>
        <view class="info">
          <view class="title">{{ r.title }}</view>
          <view class="meta">
            <text v-if="r.cookMinutes">🕐 {{ r.cookMinutes }} 分钟</text>
            <text v-if="r.nutrition?.calories">🔥 {{ r.nutrition.calories }} kcal</text>
          </view>
          <view class="tags">
            <text v-for="tag in (r.stageTags || []).slice(0, 3)" :key="tag" class="tag">{{ stageLabel(tag) }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { recipeApi, type Recipe } from '../../api/recipe'

const TABS = [
  { label: '全部', value: '' },
  { label: '月子早期', value: 'postpartum_early' },
  { label: '哺乳期', value: 'lactation' },
  { label: '月子后期', value: 'postpartum_late' },
  { label: '辅食', value: 'weaning' },
  { label: '儿童', value: 'child' }
]

const stageTag = ref('')
const keyword = ref('')
const recipes = ref<Recipe[]>([])
const loading = ref(false)
const errorMsg = ref('')
const failedImages = ref(new Set<string>())

const STAGE_LABEL: Record<string, string> = {
  postpartum_early: '月子早期',
  postpartum_late: '月子后期',
  lactation: '哺乳期',
  weaning: '辅食',
  child: '儿童'
}
const stageLabel = (t: string) => STAGE_LABEL[t] || t

async function reload() {
  if (loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await recipeApi.list({
      stageTag: stageTag.value || undefined,
      keyword: keyword.value || undefined,
      page: 1,
      size: 20
    })
    recipes.value = data.records
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '菜谱加载失败，请检查网络'
  } finally {
    loading.value = false
  }
}

function markImageFailed(id: string | number) { failedImages.value = new Set(failedImages.value).add(String(id)) }

function switchTab(v: string) {
  if (loading.value) return
  stageTag.value = v
  reload()
}

function goDetail(id: string) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}

onMounted(reload)
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; min-height: 100vh; background: #F7F7F7; }
.search-bar { display: flex; align-items: center; background: #fff; border-radius: 40rpx; padding: 8rpx 24rpx; margin-bottom: 24rpx;
  .search-input { flex: 1; height: 64rpx; font-size: 28rpx; }
  .search-btn { color: #FF8866; font-size: 28rpx; padding-left: 16rpx; }
  .search-btn.disabled { opacity: .5; }
}
.tabs { white-space: nowrap; margin-bottom: 24rpx;
  .tab { display: inline-block; padding: 16rpx 28rpx; margin-right: 16rpx; background: #fff; border-radius: 32rpx; font-size: 26rpx; color: #666;
    &.active { background: #FF8866; color: #fff; }
  }
}
.list {
  .empty { text-align: center; padding: 80rpx 0; color: #999; }
  .state-error { text-align: center; padding: 80rpx 24rpx; color: #777; font-size: 25rpx;
    button { margin-top: 24rpx; width: 220rpx; min-height: 80rpx; border-radius: 40rpx; background: #FFF0EB; color: #D94F2B; }
  }
  .card { background: #fff; border-radius: 16rpx; overflow: hidden; margin-bottom: 24rpx; display: flex;
    .cover { width: 200rpx; height: 200rpx; background: #f0f0f0; }
    .image-fallback { display: flex; align-items: center; justify-content: center; font-size: 56rpx; color: #aaa; flex-shrink: 0; }
    .info { flex: 1; padding: 24rpx;
      .title { font-size: 30rpx; font-weight: 500; }
      .meta { display: flex; gap: 16rpx; font-size: 22rpx; color: #999; margin-top: 12rpx; }
      .tags { display: flex; gap: 8rpx; margin-top: 12rpx; flex-wrap: wrap;
        .tag { font-size: 20rpx; color: #FF6644; background: #FFE5DC; padding: 4rpx 12rpx; border-radius: 8rpx; }
      }
    }
  }
}
</style>
