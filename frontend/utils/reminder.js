// 每日复盘提醒
//
// 设计说明：
//   - App（plus 环境）：使用本地通知 plus.push.createMessage。
//     该方法的 option 支持 delay（单位：秒），因此可以**预约到未来的提醒时刻**，
//     到点由系统直接弹出通知，不需要用户打开 App。
//     参考 HTML5+ Push 规范：https://www.html5plus.org/specification/Push.html
//     （MessageOption.delay：延迟一段时间显示，单位为 s，默认 0s 立即显示）
//   - 预约依赖 App 进程存活。若进程被系统强杀，预约可能失效，
//     此时由「启动补齐」在下次打开 App 时补发一条，尽量不漏提醒。
//   - H5 / 小程序：无本地通知能力，明确提示用户，不做假的「已开启」。

export const REMINDER_KEY = 'ggg_reminder'
export const LAST_FIRED_KEY = 'ggg_reminder_last_fired'
export const SCHEDULED_KEY = 'ggg_reminder_scheduled'

const DEFAULT_REMINDER = { enabled: false, time: '21:00' }
const REMIND_MSG = '该做今天的复盘了，花一分钟记下一次觉察。'

function pad2(n) {
  return String(n).padStart(2, '0')
}

function dateKey(d) {
  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
}

// 'YYYY-MM-DD HH:mm'，用于记录「已预约到哪个时刻」
function stamp(d, time) {
  return dateKey(d) + ' ' + time
}

function parseTime(time) {
  const m = /^(\d{2}):(\d{2})$/.exec(String(time || ''))
  if (!m) return { h: 21, m: 0 }
  return { h: Number(m[1]), m: Number(m[2]) }
}

// 以 baseDate 所在日期为准，构造该日期的提醒时刻
function targetOn(baseDate, time) {
  const { h, m } = parseTime(time)
  const t = new Date(baseDate.getTime())
  t.setHours(h, m, 0, 0)
  return t
}

export function loadReminder() {
  try {
    const saved = uni.getStorageSync(REMINDER_KEY)
    if (saved && typeof saved === 'object' && typeof saved.time === 'string') {
      return {
        enabled: !!saved.enabled,
        time: /^\d{2}:\d{2}$/.test(saved.time) ? saved.time : DEFAULT_REMINDER.time
      }
    }
  } catch (e) {
    // 读取失败按默认值处理
  }
  return { enabled: DEFAULT_REMINDER.enabled, time: DEFAULT_REMINDER.time }
}

export function saveReminder(reminder) {
  uni.setStorageSync(REMINDER_KEY, {
    enabled: !!reminder.enabled,
    time: reminder.time || DEFAULT_REMINDER.time
  })
  // 提醒时间变了，之前的预约标记作废，下次会按新时间重新预约
  try {
    uni.removeStorageSync(SCHEDULED_KEY)
  } catch (e) {
    // 忽略
  }
}

export function isNativeApp() {
  return typeof plus !== 'undefined' && plus.push && typeof plus.push.createMessage === 'function'
}

// 当前平台是否具备「真通知」能力
export function canNotify() {
  return isNativeApp()
}

// 申请通知权限；返回 'granted' | 'denied' | 'unsupported'
export function requestNotificationPermission() {
  return new Promise((resolve) => {
    if (!isNativeApp()) {
      resolve('unsupported')
      return
    }
    // eslint-disable-next-line no-undef
    plus.nativeUI.showWaiting('')
    try {
      // eslint-disable-next-line no-undef
      plus.push.setAutoNotification(true)
      // eslint-disable-next-line no-undef
      plus.nativeUI.closeWaiting()
      resolve('granted')
    } catch (e) {
      try {
        // eslint-disable-next-line no-undef
        plus.nativeUI.closeWaiting()
      } catch (e2) {
        // ignore
      }
      resolve('denied')
    }
  })
}

// 立即发一条本地通知
export function notifyNow(content) {
  return notifyIn(0, content)
}

/**
 * 预约一条本地通知。
 * @param {Number} delaySeconds 延迟多少秒后显示（0 = 立即）
 * @param {String} content 通知内容
 */
export function notifyIn(delaySeconds, content) {
  if (!isNativeApp()) return false
  try {
    plus.push.createMessage(content || REMIND_MSG, '', {
      title: '功过格',
      cover: false,
      sound: 'system',
      delay: Math.max(0, Math.floor(delaySeconds || 0))
    })
    return true
  } catch (e) {
    return false
  }
}

export function getLastFiredDate() {
  try {
    return uni.getStorageSync(LAST_FIRED_KEY) || ''
  } catch (e) {
    return ''
  }
}

function markFired(dateStr) {
  uni.setStorageSync(LAST_FIRED_KEY, dateStr)
}

function getScheduled() {
  try {
    return uni.getStorageSync(SCHEDULED_KEY) || ''
  } catch (e) {
    return ''
  }
}

function setScheduled(value) {
  uni.setStorageSync(SCHEDULED_KEY, value)
}

/**
 * 结算每日提醒。建议在 App 启动与回到前台时调用。
 *
 * 行为：
 *   1. 今天的提醒时刻还没到 -> 预约今天这一次（到点系统自动弹出，准点）
 *   2. 今天的提醒时刻已过   -> 补发一条（若今天还没提醒过），并预约明天
 * 通过 SCHEDULED_KEY / LAST_FIRED_KEY 去重，避免重复打扰。
 */
export function settleReminder() {
  const reminder = loadReminder()
  if (!reminder.enabled) return { action: 'disabled' }
  if (!canNotify()) return { action: 'unsupported' }

  const now = new Date()
  const todayTarget = targetOn(now, reminder.time)

  // 1) 今天还没到点：预约今天
  if (now.getTime() < todayTarget.getTime()) {
    const key = stamp(now, reminder.time)
    if (getScheduled() === key) return { action: 'already-scheduled' }
    const delay = Math.max(1, Math.floor((todayTarget.getTime() - now.getTime()) / 1000))
    const ok = notifyIn(delay, REMIND_MSG)
    if (ok) setScheduled(key)
    return { action: ok ? 'scheduled' : 'failed', delay: delay }
  }

  // 2) 今天已过点
  const today = dateKey(now)
  let fired = false
  const scheduledForToday = getScheduled() === stamp(now, reminder.time)

  if (scheduledForToday) {
    // 之前已预约今天这次，视为已提醒，不再重复补发
    if (getLastFiredDate() !== today) markFired(today)
  } else if (getLastFiredDate() !== today) {
    fired = notifyNow(REMIND_MSG)
    if (fired) markFired(today)
  }

  // 顺带预约明天，避免「明天必须打开 App 才会有提醒」
  const tomorrow = new Date(now.getTime())
  tomorrow.setDate(tomorrow.getDate() + 1)
  const nextKey = stamp(tomorrow, reminder.time)
  if (getScheduled() !== nextKey) {
    const nextTarget = targetOn(tomorrow, reminder.time)
    const delay = Math.max(1, Math.floor((nextTarget.getTime() - now.getTime()) / 1000))
    if (notifyIn(delay, REMIND_MSG)) setScheduled(nextKey)
  }

  return { action: fired ? 'fired' : 'already-fired' }
}

// 兼容旧调用名
export const settleMissedReminder = settleReminder

// 供「保存提醒」时使用：保存后立即预约
export function applyReminder(reminder) {
  saveReminder(reminder)
  if (!reminder.enabled) return { active: false }
  if (!canNotify()) return { active: false }
  const result = settleReminder()
  return {
    active: result.action === 'scheduled' || result.action === 'already-scheduled' || result.action === 'fired',
    action: result.action
  }
}
