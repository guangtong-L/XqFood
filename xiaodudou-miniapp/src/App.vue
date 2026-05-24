<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './store/user'

onLaunch(async () => {
  console.log('小肚兜 AI 启动')
  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    await userStore.loadMe()
  }
})
</script>

<style lang="scss">
page {
  background-color: #F7F7F7;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: #333;
  font-size: 28rpx;
}
</style>
