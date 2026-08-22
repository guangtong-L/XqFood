import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import { resolveAiFeatureEnabled } from '../src/config/features.ts'
import { createCountdown, formatCountdown, resetCountdown, tickCountdown, toggleCountdown } from '../src/utils/countdown.ts'

test('AI feature is closed by default and in production', () => {
  assert.equal(resolveAiFeatureEnabled(false, undefined), false)
  assert.equal(resolveAiFeatureEnabled(false, 'true'), false)
})

test('AI feature requires development mode and explicit flag', () => {
  assert.equal(resolveAiFeatureEnabled(true, undefined), false)
  assert.equal(resolveAiFeatureEnabled(true, 'false'), false)
  assert.equal(resolveAiFeatureEnabled(true, 'true'), true)
})

test('countdown validates boundaries and formats visible time', () => {
  assert.throws(() => createCountdown(0), /无效/)
  assert.throws(() => createCountdown(1.5), /无效/)
  assert.throws(() => createCountdown(86401), /无效/)
  assert.equal(formatCountdown(65), '01:05')
  assert.equal(formatCountdown(3661), '01:01:01')
})

test('countdown supports pause continue finish and reset without hidden progress', () => {
  let state = createCountdown(2)
  state = toggleCountdown(state)
  assert.strictEqual(tickCountdown(state), state)
  state = toggleCountdown(state)
  state = tickCountdown(state)
  assert.equal(state.remainingSeconds, 1)
  state = tickCountdown(state)
  assert.deepEqual({ remaining: state.remainingSeconds, running: state.running, finished: state.finished },
    { remaining: 0, running: false, finished: true })
  state = resetCountdown(state)
  assert.deepEqual({ remaining: state.remainingSeconds, running: state.running, finished: state.finished },
    { remaining: 2, running: true, finished: false })
})

test('core pages distinguish network errors from empty data and expose retry', () => {
  for (const page of ['index/index.vue', 'recipe/list.vue', 'recipe/detail.vue', 'me/favorites.vue', 'me/checkin.vue']) {
    const source = readFileSync(new URL(`../src/pages/${page}`, import.meta.url), 'utf8')
    assert.match(source, /error|Error/)
    assert.match(source, /重试/)
  }
  const favorites = readFileSync(new URL('../src/pages/me/favorites.vue', import.meta.url), 'utf8')
  assert.ok(favorites.indexOf('errorMsg && recipes.length === 0') < favorites.indexOf('recipes.length === 0" class="empty'))
})

test('core remote images have failure placeholders and write actions have pending locks', () => {
  for (const page of ['index/index.vue', 'recipe/list.vue', 'recipe/detail.vue', 'me/favorites.vue', 'me/checkin.vue']) {
    const source = readFileSync(new URL(`../src/pages/${page}`, import.meta.url), 'utf8')
    assert.match(source, /@error/)
    assert.match(source, /fallback|Failed/)
  }
  assert.match(readFileSync(new URL('../src/pages/recipe/detail.vue', import.meta.url), 'utf8'), /favoriteLoading/)
  assert.match(readFileSync(new URL('../src/pages/me/favorites.vue', import.meta.url), 'utf8'), /pendingIds/)
  assert.match(readFileSync(new URL('../src/pages/me/checkin.vue', import.meta.url), 'utf8'), /deletingIds/)
})

test('stale favorite and calendar data are explicitly marked or rolled back after refresh failure', () => {
  const favorites = readFileSync(new URL('../src/pages/me/favorites.vue', import.meta.url), 'utf8')
  assert.match(favorites, /刷新失败，当前为上次结果/)
  assert.match(favorites, /加载更多失败，当前列表未更新/)

  const checkin = readFileSync(new URL('../src/pages/me/checkin.vue', import.meta.url), 'utf8')
  assert.match(checkin, /月份加载失败，已恢复原月份/)
  assert.match(checkin, /curYear\.value = previousYear/)
  assert.match(checkin, /curMonth\.value = previousMonth/)
  assert.match(checkin, /requestedMonth !== monthStr\.value/)
})

test('login and privacy copy do not claim per-user customization while production AI is closed', () => {
  const login = readFileSync(new URL('../src/pages/auth/wx-login.vue', import.meta.url), 'utf8')
  const privacy = readFileSync(new URL('../src/pages/legal/privacy.vue', import.meta.url), 'utf8')
  assert.doesNotMatch(login, /每一口.*定制|个性化/)
  assert.match(login, /记录、收藏并查看母婴阶段菜谱/)
  assert.doesNotMatch(privacy, /提供个性化菜谱推荐和营养分析/)
})
