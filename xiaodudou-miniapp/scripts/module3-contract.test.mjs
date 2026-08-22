import assert from 'node:assert/strict'
import test from 'node:test'
import { buildCheckinInput, localDateDaysAgo, validateServings } from '../src/utils/checkin.ts'

test('checkin servings accepts boundaries and quarter portions', () => {
  assert.equal(validateServings(0.25), true)
  assert.equal(validateServings(1.5), true)
  assert.equal(validateServings(10), true)
})

test('checkin servings rejects invalid ranges and precision', () => {
  assert.equal(validateServings(0.24), false)
  assert.equal(validateServings(10.01), false)
  assert.equal(validateServings(1.234), false)
})

test('checkin input always contains explicit meal and servings', () => {
  assert.deepEqual(buildCheckinInput('2001', 'lunch', 1.25), {
    recipeId: '2001', mealType: 'lunch', servings: 1.25
  })
  assert.throws(() => buildCheckinInput('2001', 'lunch', 0), /份数/)
})

test('checkin date helper uses local calendar days instead of UTC date truncation', () => {
  const local = new Date(2026, 0, 8, 0, 30)
  assert.equal(localDateDaysAgo(0, local), '2026-01-08')
  assert.equal(localDateDaysAgo(7, local), '2026-01-01')
})
