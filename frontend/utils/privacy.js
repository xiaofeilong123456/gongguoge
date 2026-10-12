// 用户协议与隐私政策同意状态
//
// 合规要求（策划书 §3.2）：首次启动必须主动展示协议与隐私政策，
// 用户明示同意后方可进入应用；不同意则退出，不得静默视为同意。
//
// 采用版本号管理：协议内容有实质变更时递增 PRIVACY_VERSION，
// 已同意旧版本的用户会被再次要求确认。

const CONSENT_KEY = 'ggg_privacy_consent'

// 协议版本：内容发生实质变更时递增
export const PRIVACY_VERSION = '1.0'

export function getConsent() {
  try {
    const saved = uni.getStorageSync(CONSENT_KEY)
    if (saved && typeof saved === 'object') {
      return {
        version: saved.version || '',
        agreedAt: saved.agreedAt || ''
      }
    }
  } catch (e) {
    // 读取失败按未同意处理
  }
  return { version: '', agreedAt: '' }
}

// 是否已同意「当前版本」的协议
export function hasAgreedPrivacy() {
  return getConsent().version === PRIVACY_VERSION
}

export function agreePrivacy() {
  const record = {
    version: PRIVACY_VERSION,
    agreedAt: new Date().toISOString()
  }
  uni.setStorageSync(CONSENT_KEY, record)
  return record
}

// 清除同意状态（用于「设置 - 撤回同意」或测试）
export function resetPrivacy() {
  uni.removeStorageSync(CONSENT_KEY)
}

export function formatAgreedAt(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
    ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
}
