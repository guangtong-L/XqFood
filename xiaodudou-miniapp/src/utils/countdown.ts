export interface CountdownState {
  initialSeconds: number
  remainingSeconds: number
  running: boolean
  finished: boolean
}

export function isValidCountdownSeconds(seconds: unknown): seconds is number {
  return typeof seconds === 'number' && Number.isInteger(seconds) && seconds >= 1 && seconds <= 86400
}

export function createCountdown(seconds: unknown): CountdownState {
  if (!isValidCountdownSeconds(seconds)) throw new Error('计时秒数无效')
  return { initialSeconds: seconds, remainingSeconds: seconds, running: true, finished: false }
}

export function tickCountdown(state: CountdownState): CountdownState {
  if (!state.running || state.finished) return state
  const remainingSeconds = Math.max(0, state.remainingSeconds - 1)
  return {
    ...state,
    remainingSeconds,
    running: remainingSeconds > 0,
    finished: remainingSeconds === 0
  }
}

export function toggleCountdown(state: CountdownState): CountdownState {
  if (state.finished) return state
  return { ...state, running: !state.running }
}

export function resetCountdown(state: CountdownState): CountdownState {
  return { ...state, remainingSeconds: state.initialSeconds, running: true, finished: false }
}

export function formatCountdown(seconds: number): string {
  const safe = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  const secs = safe % 60
  return [hours, minutes, secs]
    .filter((_, index) => hours > 0 || index > 0)
    .map(value => String(value).padStart(2, '0'))
    .join(':')
}
