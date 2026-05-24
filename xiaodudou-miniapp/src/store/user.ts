import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userApi, type User, type UserProfile } from '../api/user'
import { TOKEN_STORAGE_KEY } from '../utils/request'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const profile = ref<UserProfile | null>(null)
  const isLoggedIn = ref<boolean>(!!uni.getStorageSync(TOKEN_STORAGE_KEY))

  async function loadMe() {
    try {
      const data = await userApi.me()
      user.value = data.user
      profile.value = data.profile
      isLoggedIn.value = true
    } catch (e) {
      isLoggedIn.value = false
    }
  }

  function setToken(token: string) {
    uni.setStorageSync(TOKEN_STORAGE_KEY, token)
    isLoggedIn.value = true
  }

  function logout() {
    uni.removeStorageSync(TOKEN_STORAGE_KEY)
    user.value = null
    profile.value = null
    isLoggedIn.value = false
  }

  function setProfile(p: UserProfile) {
    profile.value = p
  }

  return { user, profile, isLoggedIn, loadMe, setToken, setProfile, logout }
})
