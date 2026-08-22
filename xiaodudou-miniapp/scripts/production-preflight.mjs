import { readFileSync } from 'node:fs'
import { validateTabBarAssets } from './tabbar-assets.mjs'

const manifest = JSON.parse(readFileSync(new URL('../src/manifest.json', import.meta.url), 'utf8'))
const errors = []
errors.push(...validateTabBarAssets())
const appid = manifest?.['mp-weixin']?.appid || ''

if (!appid || appid.startsWith('TODO_')) errors.push('微信小程序 AppID 尚未配置')
if (manifest?.['mp-weixin']?.setting?.urlCheck !== true) errors.push('微信合法域名校验必须开启')

const apiBaseUrl = (process.env.VITE_API_BASE_URL || '').trim()
if (!apiBaseUrl) {
  errors.push('缺少 VITE_API_BASE_URL')
} else {
  try {
    const parsed = new URL(apiBaseUrl)
    if (parsed.protocol !== 'https:' || ['localhost', '127.0.0.1'].includes(parsed.hostname.toLowerCase())) {
      errors.push('VITE_API_BASE_URL 必须使用 HTTPS 且不能指向 localhost')
    }
  } catch {
    errors.push('VITE_API_BASE_URL 不是有效 URL')
  }
}

if (errors.length) {
  console.error(`生产上线前置检查未通过：\n - ${errors.join('\n - ')}`)
  process.exit(1)
}

console.log('生产上线前置检查通过')
