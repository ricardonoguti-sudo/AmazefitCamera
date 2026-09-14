export const TAKE_PHOTO = 'TAKE_PHOTO'
export const TIMER_OPTIONS = [0, 3, 5, 10]

export function commandPayload(delaySeconds = 0) {
  return {
    type: 'camera-command',
    name: TAKE_PHOTO,
    delaySeconds,
    createdAt: Date.now(),
  }
}
