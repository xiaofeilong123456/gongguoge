import { SERVER_HOST, SERVER_PORT, SERVER_HTTPS } from './config.js'

// 推断后端地址。
//
// 规则：
//   1. 浏览器里通过 IP / 域名访问时，跟随地址栏主机名（方便换网络环境）；
//   2. 其余情况（电脑浏览器用 localhost、手机真机、小程序、App）统一用
//      config.js 里配置的局域网 IP —— 因为手机上的 localhost 指向手机自己。
function detectBaseUrl() {
  try {
    if (typeof window !== 'undefined' && window.location) {
      const host = window.location.hostname
      if (host && host !== 'localhost' && host !== '127.0.0.1' && host !== 'file') {
        return 'http://' + host + ':' + SERVER_PORT + '/api'
      }
    }
  } catch (e) {
    // 忽略，走下面的兜底
  }
  const protocol = SERVER_HTTPS ? 'https://' : 'http://'
  return protocol + SERVER_HOST + ':' + SERVER_PORT + '/api'
}

export const BASE_URL = detectBaseUrl()

// 后端服务的根地址（去掉 /api 后缀），用于拼接静态资源（如头像）的完整 URL
export const SERVER_URL = BASE_URL.replace(/\/api\/?$/, '')

// 把后端返回的相对路径（如 /uploads/avatar/xxx.png）转成可直接用于 <image src> 的地址
export function toAbsoluteUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return SERVER_URL + (path.startsWith('/') ? path : '/' + path)
}

export function getToken() {
  return uni.getStorageSync('ggg_token') || ''
}

export function setToken(token) {
  uni.setStorageSync('ggg_token', token)
}

export function clearToken() {
  uni.removeStorageSync('ggg_token')
}

// 登录失效时统一跳登录页。
// 加锁是为了避免多个并发请求同时 401 时反复 reLaunch。
let redirectingToLogin = false

function redirectToLogin() {
  if (redirectingToLogin) return
  // 已经在登录页就不要再 reLaunch，否则会和「自动登录」来回跳
  try {
    const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
    const current = pages.length ? pages[pages.length - 1].route : ''
    if (current === 'pages/auth/auth') {
      clearToken()
      return
    }
  } catch (e) {
    // 忽略：某些平台不支持 getCurrentPages
  }
  redirectingToLogin = true
  clearToken()
  try {
    uni.reLaunch({
      url: '/pages/auth/auth',
      complete: () => {
        setTimeout(() => { redirectingToLogin = false }, 1500)
      }
    })
  } catch (e) {
    redirectingToLogin = false
  }
}

export function request(options) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: (options.baseUrl || BASE_URL) + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(getToken() ? { Authorization: 'Bearer ' + getToken() } : {}),
        ...(options.header || {})
      },
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
        } else if (body.code === 401 || res.statusCode === 401) {
          // 后端为保持统一响应格式，用的是 HTTP 200 + body.code=401，
          // 所以这里必须同时判断 body.code，否则 token 过期不会跳登录页。
          redirectToLogin()
          reject(body)
        } else {
          reject(body)
        }
      },
      fail: reject
    })
  })
}

export function uploadFile(options) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: (options.baseUrl || BASE_URL) + options.url,
      filePath: options.filePath,
      name: options.name || 'file',
      header: getToken() ? { Authorization: 'Bearer ' + getToken() } : {},
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          resolve(data.data || data)
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}
