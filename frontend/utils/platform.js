// 平台能力封装
//
// 集中处理「某个 API 在当前平台是否可用」，避免各页面散落条件编译判断。

// 是否运行在 App（原生，存在 plus 环境）
export function isApp() {
  return typeof plus !== 'undefined'
}

// 是否运行在 H5（浏览器）。用运行时特性检测，避免条件编译在函数体内造成返回值歧义。
export function isH5() {
  return typeof window !== 'undefined' && typeof document !== 'undefined'
}

// 是否具备本地通知能力（依赖 plus.push）
export function canLocalNotify() {
  return typeof plus !== 'undefined' && plus.push && typeof plus.push.createMessage === 'function'
}

// 退出应用。仅 App 端支持直接退出；
// H5 / 小程序无法主动关闭，返回 false 由调用方决定如何提示。
export function quitApp() {
  if (typeof plus !== 'undefined' && plus.runtime && typeof plus.runtime.quit === 'function') {
    plus.runtime.quit()
    return true
  }
  return false
}
