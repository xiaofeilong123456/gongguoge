// 权限申请前的用途说明
//
// 合规要求（策划书 §3.2 / §10.3）：
//   「所有权限申请（存储、通知）前弹出说明弹窗，明确告知权限用途」
//   即：不允许直接调起系统权限弹窗，必须先由应用自己解释清楚为什么需要。
//
// 本模块只负责「说明文案 + 是否已说明过」，
// 具体的权限调用仍由各功能模块负责（见 utils/reminder.js）。

import { isH5 } from './platform.js'

const SHOWN_KEY = 'ggg_permission_shown'

// 各权限的说明文案。用途必须具体，不能写「为了更好的体验」这类空话。
export const PERMISSION_COPY = {
  notification: {
    title: '通知权限说明',
    purpose: '用于在你设定的时间提醒你完成当天的复盘记录。',
    detail: '这是本地通知，由手机系统在你的设备上生成，不会把你的记录内容上传给任何第三方。',
    refuse: '拒绝后仍可正常记录与查看统计，只是不会收到定时提醒；你可以随时在系统设置中重新开启。',
    confirm: '去开启',
    cancel: '暂不开启'
  },
  storage: {
    title: '存储权限说明',
    purpose: '用于把导出的数据文件保存到本机，以及缓存离线记录。',
    detail: '导出文件仅保存在你的设备上，应用不会读取与功过格无关的其他文件。',
    refuse: '拒绝后仍可使用全部记录功能，但可能无法保存导出文件。',
    confirm: '去开启',
    cancel: '暂不开启'
  },
  export: {
    title: '数据导出说明',
    purpose: '用于把你在功过格里的记录导出到本机，方便自行留存或备份。',
    detail: '导出的内容会先复制到系统剪贴板，你可以粘贴到备忘录或文件中保存；数据不会因此上传到任何服务器。',
    refuse: '取消不会有任何影响，你随时可以在设置里重新导出。',
    confirm: '继续导出',
    cancel: '取消'
  }
}

function loadShown() {
  try {
    const saved = uni.getStorageSync(SHOWN_KEY)
    return saved && typeof saved === 'object' ? saved : {}
  } catch (e) {
    return {}
  }
}

export function hasShownPermission(key) {
  return !!loadShown()[key]
}

export function markPermissionShown(key) {
  const shown = loadShown()
  shown[key] = new Date().toISOString()
  uni.setStorageSync(SHOWN_KEY, shown)
}

export function resetPermissionShown() {
  uni.removeStorageSync(SHOWN_KEY)
}

// 供「权限管理」页展示：每条权限的用途，以及是否已向用户说明过
export function permissionOverview() {
  const shown = loadShown()
  return Object.keys(PERMISSION_COPY).map((key) => ({
    key,
    title: PERMISSION_COPY[key].title,
    purpose: PERMISSION_COPY[key].purpose,
    explainedAt: shown[key] || ''
  }))
}

/**
 * 在申请权限前弹出说明。返回 true 表示用户同意继续申请。
 * 无论用户同意还是拒绝，都记为「已说明」，避免反复打扰。
 */
export function explainPermission(key) {
  let copy = PERMISSION_COPY[key]
  if (!copy) return Promise.resolve(false)

  // 导出方式随平台不同（H5 直接下载文件，App 端复制到剪贴板），
  // 说明文案必须与实际行为一致，不能笼统描述。
  if (key === 'export') {
    copy = Object.assign({}, copy, {
      detail: isH5()
        ? '导出的内容会生成一个文件直接下载到本机。数据不会因此上传到任何服务器。'
        : '导出的内容会先复制到系统剪贴板，你可以粘贴到备忘录或文件中保存；数据不会因此上传到任何服务器。'
    })
  }

  return new Promise((resolve) => {
    uni.showModal({
      title: copy.title,
      content: copy.purpose + '\n\n' + copy.detail + '\n\n' + copy.refuse,
      confirmText: copy.confirm,
      cancelText: copy.cancel,
      confirmColor: '#367d57',
      success: (res) => {
        markPermissionShown(key)
        resolve(!!res.confirm)
      },
      fail: () => {
        markPermissionShown(key)
        resolve(false)
      }
    })
  })
}
