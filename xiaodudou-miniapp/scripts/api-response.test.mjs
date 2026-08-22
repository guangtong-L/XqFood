import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError, evaluateApiResponse } from '../src/utils/api-response.ts'

test('2xx success returns data', () => {
  assert.deepEqual(evaluateApiResponse(200, { code: 0, message: '成功', data: { ok: true } }), { ok: true })
})

for (const status of [400, 401, 403, 404, 429, 503]) {
  test(`HTTP ${status} keeps backend message and code`, () => {
    assert.throws(
      () => evaluateApiResponse(status, { code: 10001 + status, message: `错误${status}` }),
      (error) => error instanceof ApiError && error.statusCode === status
        && error.code === 10001 + status && error.message === `错误${status}`
    )
  })
}

test('200 business error remains an error', () => {
  assert.throws(() => evaluateApiResponse(200, { code: 40001, message: '用户不存在' }), ApiError)
})

test('non-json response is rejected consistently', () => {
  assert.throws(() => evaluateApiResponse(502, '<html>bad gateway</html>'), /HTTP 502/)
})
