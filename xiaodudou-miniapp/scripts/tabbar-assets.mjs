import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const PNG_MAGIC = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])

const scriptDirectory = fileURLToPath(new URL('.', import.meta.url))

export function validateTabBarAssets(projectRoot = resolve(scriptDirectory, '..')) {
  const pagesPath = resolve(projectRoot, 'src/pages.json')
  const pages = JSON.parse(readFileSync(pagesPath, 'utf8'))
  const errors = []
  for (const [index, item] of (pages.tabBar?.list || []).entries()) {
    for (const field of ['iconPath', 'selectedIconPath']) {
      const relativePath = item?.[field]
      if (!relativePath) {
        errors.push(`tabBar.list[${index}].${field} 未配置`)
        continue
      }
      const absolutePath = resolve(projectRoot, 'src', relativePath)
      try {
        if (statSync(absolutePath).size <= PNG_MAGIC.length) {
          errors.push(`${relativePath} 为空或过小`)
          continue
        }
        const header = readFileSync(absolutePath).subarray(0, PNG_MAGIC.length)
        if (!header.equals(PNG_MAGIC)) errors.push(`${relativePath} 不是有效 PNG 文件`)
      } catch {
        errors.push(`${relativePath} 不存在`)
      }
    }
  }
  return errors
}
