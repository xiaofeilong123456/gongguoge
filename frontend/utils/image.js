// 图片地址解析
//
// 背景（真机调试踩过的坑）：
//   微信小程序的 <image> 组件在真机上**不接受 http:// 图片**（要求 HTTPS），
//   即使开发者工具里勾了「不校验合法域名」也一样；
//   但 uni.request 是放行的。
//   所以这里对 http 图片改用 uni.request 拉二进制、转成 base64 data URI 再显示，
//   data URI 不经过网络请求，不受域名规则约束。
//
// H5 端浏览器本身能直接显示 http 图片，无需转换。

import { toAbsoluteUrl } from './request.js'
import { isH5 } from './platform.js'

// 已转换过的图片缓存，避免每次进入页面都重新下载
const dataUrlCache = {}

function detectMime(url) {
  const lower = String(url).toLowerCase()
  if (lower.endsWith('.png')) return 'image/png'
  if (lower.endsWith('.webp')) return 'image/webp'
  if (lower.endsWith('.gif')) return 'image/gif'
  return 'image/jpeg'
}

function arrayBufferToBase64(buffer) {
  // 小程序提供原生方法
  if (typeof wx !== 'undefined' && typeof wx.arrayBufferToBase64 === 'function') {
    return wx.arrayBufferToBase64(buffer)
  }
  // H5 / App：分块转换，避免超长字符串导致栈溢出
  const bytes = new Uint8Array(buffer)
  let binary = ''
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize))
  }
  if (typeof btoa === 'function') {
    return btoa(binary)
  }
  return ''
}

/**
 * 把后端返回的相对图片路径解析成可直接用于 <image src> 的值。
 * 返回 Promise<string>，失败时返回空串。
 */
export function resolveImageSrc(path) {
  const url = toAbsoluteUrl(path)
  if (!url) return Promise.resolve('')
  // 非 http（https / data: / 本地文件）直接使用
  if (!/^http:\/\//i.test(url)) return Promise.resolve(url)
  // H5 浏览器可直接显示 http 图片
  if (isH5()) return Promise.resolve(url)
  // 小程序 / App 真机：转 base64
  if (dataUrlCache[url]) return Promise.resolve(dataUrlCache[url])

  return new Promise((resolve) => {
    try {
      uni.request({
        url,
        method: 'GET',
        responseType: 'arraybuffer',
        success: (res) => {
          try {
            if (res.statusCode !== 200 || !res.data) {
              resolve('')
              return
            }
            const base64 = arrayBufferToBase64(res.data)
            if (!base64) {
              resolve('')
              return
            }
            const dataUrl = 'data:' + detectMime(url) + ';base64,' + base64
            dataUrlCache[url] = dataUrl
            resolve(dataUrl)
          } catch (e) {
            resolve('')
          }
        },
        fail: () => resolve('')
      })
    } catch (e) {
      resolve('')
    }
  })
}

// 清空缓存（例如更换头像后需要重新拉取）
export function clearImageCache() {
  Object.keys(dataUrlCache).forEach((k) => {
    delete dataUrlCache[k]
  })
}
