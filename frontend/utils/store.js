// 本地轻量存储工具
//
// 说明：本模块只做「日期工具 + 本地用户资料缓存」两件事。
// 记录、分类、回收站、反馈全部走后端接口（见 utils/api.js），
// 不再保留任何本地假数据模型，避免页面误接数据源。

const PROFILE_KEY = 'ggg_profile'

function pad2(value) {
  return String(value).padStart(2, '0')
}

export function formatDate(date) {
  return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate())
}

export function todayDate() {
  return formatDate(new Date())
}

// 本地缓存的用户资料仅用于「接口失败时兜底展示」与「未登录时的占位」，
// 不预置任何虚构的手机号或统计数字。
const EMPTY_PROFILE = {
  nickname: '功过格用户',
  phone: '',
  username: '',
  avatar: ''
}

export function getProfile() {
  try {
    const saved = uni.getStorageSync(PROFILE_KEY)
    if (saved && typeof saved === 'object') {
      return {
        nickname: saved.nickname || EMPTY_PROFILE.nickname,
        phone: saved.phone || '',
        username: saved.username || '',
        avatar: saved.avatar || ''
      }
    }
  } catch (e) {
    // 读取失败返回占位资料
  }
  return { ...EMPTY_PROFILE }
}

export function saveProfile(profile) {
  const safe = profile && typeof profile === 'object' ? profile : {}
  uni.setStorageSync(PROFILE_KEY, {
    nickname: safe.nickname || EMPTY_PROFILE.nickname,
    phone: safe.phone || '',
    username: safe.username || '',
    avatar: safe.avatar || ''
  })
}

// 退出登录时清除本地缓存的用户资料，避免下个账号看到上个人的昵称头像
export function clearProfile() {
  try {
    uni.removeStorageSync(PROFILE_KEY)
  } catch (e) {
    // 忽略
  }
  return { ...EMPTY_PROFILE }
}

export function maskPhone(phone) {
  const value = String(phone || '')
  return value.length === 11 ? value.slice(0, 3) + '****' + value.slice(7) : value
}
