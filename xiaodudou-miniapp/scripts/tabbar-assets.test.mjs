import assert from 'node:assert/strict'
import test from 'node:test'
import { validateTabBarAssets } from './tabbar-assets.mjs'

test('every tabBar normal and selected icon exists and has PNG magic', () => {
  assert.deepEqual(validateTabBarAssets(), [])
})
