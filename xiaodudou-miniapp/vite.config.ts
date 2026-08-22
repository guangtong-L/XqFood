import { defineConfig, loadEnv } from 'vite'
import uniPluginModule from '@dcloudio/vite-plugin-uni'

// DCloud 5.24 的插件仍以 CommonJS 发布；ESM 项目下 Node 会把 default 再包一层。
const uni = typeof uniPluginModule === 'function'
  ? uniPluginModule
  : (uniPluginModule as unknown as { default: typeof uniPluginModule }).default

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = process.env.VITE_API_BASE_URL || env.VITE_API_BASE_URL || ''
  if (command === 'build') {
    if ((process.env.VITE_AI_FEATURE_ENABLED || env.VITE_AI_FEATURE_ENABLED) === 'true') {
      throw new Error('生产构建禁止开启 AI：真实内容安全审核供应商尚未完成接入验收')
    }
    if (!apiBaseUrl) throw new Error('生产构建缺少 VITE_API_BASE_URL')
    const parsed = new URL(apiBaseUrl)
    if (parsed.protocol !== 'https:' || ['localhost', '127.0.0.1'].includes(parsed.hostname.toLowerCase())) {
      throw new Error('生产 VITE_API_BASE_URL 必须使用 HTTPS 且不能指向 localhost')
    }
  }
  return {
    plugins: [uni()],
    css: {
      preprocessorOptions: {
        scss: {
          // DCloud 5.24/Vite 5 仍通过 Sass legacy API 调用；仅静默该已知上游弃用项。
          silenceDeprecations: ['legacy-js-api'],
        },
      },
    },
  }
})
