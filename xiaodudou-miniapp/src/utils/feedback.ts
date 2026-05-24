/**
 * 全局反馈工具 - 统一 loading / toast 体验
 */

let loadingCount = 0

export const feedback = {
  /** 显示加载，多次调用计数 */
  loading(title = '加载中...') {
    loadingCount++
    uni.showLoading({ title, mask: true })
  },

  /** 隐藏加载（计数归零才真正隐藏） */
  hideLoading(force = false) {
    if (force) {
      loadingCount = 0
      uni.hideLoading()
      return
    }
    loadingCount = Math.max(0, loadingCount - 1)
    if (loadingCount === 0) uni.hideLoading()
  },

  toast(msg: string, icon: 'success' | 'error' | 'none' = 'none', duration = 2000) {
    uni.showToast({ title: msg, icon, duration, mask: false })
  },

  success(msg: string) { this.toast(msg, 'success') },
  error(msg: string) { this.toast(msg, 'error') },

  /** 二次确认对话框，返回 boolean */
  confirm(content: string, title = '提示'): Promise<boolean> {
    return new Promise((resolve) => {
      uni.showModal({
        title,
        content,
        success: (r) => resolve(!!r.confirm),
        fail: () => resolve(false)
      })
    })
  }
}
