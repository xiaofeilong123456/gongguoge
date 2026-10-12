// 云端同步集成测试（策划书 §2.2.4 / §2.3「本地优先 + 恢复网络自动同步」）
//
// 与 sync-test.mjs 的区别：
//   sync-test.mjs  只打接口，验证「服务端」的幂等与操作语义；
//   本脚本        同时加载**真实的前端同步模块**（offline.js / sync.js / api.js / request.js），
//                 只替换 uni API，因此能验证「客户端离线队列」的真实行为：
//                 断网入队 -> 恢复网络自动补传 -> 重复提交不写重 -> 换设备自动拉取。
//
// 运行前需先启动后端（默认 8080），然后：
//   node frontend/scripts/sync-e2e-test.mjs
// 若后端在别的端口：
//   set GGG_API_PORT=8081 && node frontend/scripts/sync-e2e-test.mjs
//
// 会创建一个昵称为「测试账号」的临时账号，结束时不会自动删除，
// 可用 backend/scripts/clean-test-data.mjs 清理。

import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const UTILS = path.join(__dirname, '..', 'utils') + path.sep

const API_PORT = Number(process.env.GGG_API_PORT || 8080)
const API = 'http://127.0.0.1:' + API_PORT + '/api'

// ---------------- 可控的 uni 环境 ----------------
const storage = new Map()
let online = true
let netCallback = null

// 清空本地存储 = 模拟「换了一台新设备」
function newDevice() {
  storage.clear()
}

function mockRequest(opts) {
  let url = String(opts.url)
  // 配置里 BASE_URL 指向 8080，测试端口不同时改写
  if (API_PORT !== 8080) url = url.replace(':8080/', ':' + API_PORT + '/')
  const method = (opts.method || 'GET').toUpperCase()
  if (!online) {
    setTimeout(() => opts.fail && opts.fail({ errMsg: 'request:fail' }), 0)
    return
  }
  let body
  if (method === 'GET') {
    const qs = new URLSearchParams(opts.data || {}).toString()
    if (qs) url += (url.includes('?') ? '&' : '?') + qs
  } else if (opts.data !== undefined) {
    body = JSON.stringify(opts.data)
  }
  fetch(url, { method, headers: Object.assign({}, opts.header), body })
    .then(async (r) => {
      const text = await r.text()
      let data = null
      try { data = JSON.parse(text) } catch { data = { raw: text } }
      opts.success && opts.success({ statusCode: r.status, data })
    })
    .catch((e) => opts.fail && opts.fail(e))
}

globalThis.uni = {
  getStorageSync: (k) => (storage.has(k) ? storage.get(k) : ''),
  setStorageSync: (k, v) => storage.set(k, v),
  removeStorageSync: (k) => storage.delete(k),
  onNetworkStatusChange: (cb) => { netCallback = cb },
  request: mockRequest,
  showToast: () => {},
  showLoading: () => {},
  hideLoading: () => {},
  reLaunch: () => {},
  navigateTo: () => {}
}

const url = (f) => 'file://' + UTILS.replace(/\\/g, '/') + f
const offline = await import(url('offline.js'))
const sync = await import(url('sync.js'))
const api = await import(url('api.js'))

let pass = 0, fail = 0
const t = (n, ok, d) => { if (ok) { pass++; console.log('PASS  ' + n) } else { fail++; console.log('FAIL  ' + n + (d ? '  -> ' + d : '')) } }

async function serverGet(p) {
  const r = await fetch(API + p, { headers: { Authorization: 'Bearer ' + token } })
  const j = await r.json()
  return j.data
}

// ---------------- 准备账号 ----------------
const phone = '138' + String(Date.now()).slice(-8)
await fetch(API + '/auth/sms-code', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone, scene: 'login' }) })
const loginRes = await (await fetch(API + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone, code: '123456', nickname: '测试账号' }) })).json()
if (!loginRes.data || !loginRes.data.token) {
  console.error('登录失败，请确认后端已启动：' + API)
  console.error(JSON.stringify(loginRes))
  process.exit(1)
}
const token = loginRes.data.token
const userId = loginRes.data.userInfo.id
console.log('后端: ' + API)
console.log('测试账号: ' + phone + '  userId=' + userId + '\n')

const today = (() => {
  const d = new Date(); const p = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
})()

function useAccount() {
  storage.set('ggg_token', token)
  offline.setOfflineScope(userId)
}

// =========================================================
console.log('===== 场景 1：断网时记录（本地优先，不丢） =====')
useAccount()
online = false
const cats = (await serverGet('/categories')) || []
const posCat = cats.find(c => c.type === 1)
const refCat = cats.find(c => c.type === 2)
const ids = ['sync_e2e_a', 'sync_e2e_b', 'sync_e2e_c']
ids.forEach((cid, i) => {
  offline.enqueue({ clientId: cid, op: 'create', type: 1, categoryId: posCat && posCat.id, categoryName: posCat && posCat.name, content: '离线记录' + (i + 1), recordDate: today })
  offline.upsertLocalRecord({ clientId: cid, type: 1, content: '离线记录' + (i + 1), recordDate: today })
})
t('断网时 3 条操作全部入队（不丢）', offline.queueSize() === 3, '队列=' + offline.queueSize())
t('本地镜像 3 条待同步', offline.getLocalRecords().filter(r => r.pending).length === 3)
const offlineFlush = await sync.flushQueue()
t('断网冲刷不丢数据（队列保留）', offlineFlush.flushed === 0 && offline.queueSize() === 3, JSON.stringify(offlineFlush))
t('此时服务端还没有这些记录', (await serverGet('/records/today')).list.filter(r => /离线记录/.test(r.content)).length === 0)

// =========================================================
console.log('\n===== 场景 2：恢复网络自动补传 =====')
online = true
const resumed = await sync.syncOnResume()
t('恢复网络后自动冲刷成功 3 条', resumed.flushed === 3, JSON.stringify(resumed))
t('队列已清空', offline.queueSize() === 0)
t('本地镜像已标记同步（不再 pending）', offline.getLocalRecords().filter(r => r.pending).length === 0)
t('云端确实收到 3 条', (await serverGet('/records/today')).list.filter(r => /离线记录/.test(r.content)).length === 3)

// =========================================================
console.log('\n===== 场景 3：重复提交幂等（不会写重） =====')
ids.forEach((cid, i) => offline.enqueue({ clientId: cid, op: 'create', type: 1, categoryId: posCat.id, content: '离线记录' + (i + 1), recordDate: today }))
const again = await sync.flushQueue()
const dupCheck = (await serverGet('/records/today')).list.filter(r => /离线记录/.test(r.content))
t('重复提交后云端仍只有 3 条', dupCheck.length === 3, '实际 ' + dupCheck.length + ' 条')
t('重复提交被识别为已存在（未新增）', again.flushed === 3, JSON.stringify(again))

// =========================================================
console.log('\n===== 场景 4：换设备登录自动拉取 =====')
newDevice()
useAccount()
t('新设备本地无数据', offline.getLocalRecords().length === 0)
const pulled = await api.recordApi.today()
t('登录后从云端拉到 3 条记录', ((pulled && pulled.list) || []).filter(r => /离线记录/.test(r.content)).length === 3)
const pulledStats = await api.statisticsApi.get({ range: 7 })
t('统计接口同样能拉到（换设备统计正确）', pulledStats && pulledStats.overview && pulledStats.overview.total >= 3,
  JSON.stringify(pulledStats && pulledStats.overview))

// =========================================================
console.log('\n===== 场景 5：离线删除也能同步 =====')
online = false
offline.enqueue({ clientId: ids[0], op: 'delete' })
t('断网时删除入队', offline.queueSize() === 1)
online = true
t('恢复网络后删除同步成功', (await sync.flushQueue()).flushed === 1)
t('云端今日列表少了一条', (await serverGet('/records/today')).list.filter(r => /离线记录/.test(r.content)).length === 2)
t('删除的记录进了回收站（可恢复）', ((await serverGet('/records/recycle')) || []).some(r => /离线记录/.test(r.content)))

// =========================================================
console.log('\n===== 场景 6：网络恢复事件触发自动同步 =====')
sync.bindNetworkListener()
t('已注册网络状态监听', typeof netCallback === 'function')
newDevice()
useAccount()
online = true
offline.enqueue({ clientId: 'sync_e2e_net', op: 'create', type: 2, categoryId: refCat && refCat.id, content: '监听触发记录', recordDate: today })
t('入队 1 条待同步', offline.queueSize() === 1)
if (netCallback) netCallback({ isConnected: true })
await new Promise(r => setTimeout(r, 1000))
t('网络恢复事件触发了同步（队列清空）', offline.queueSize() === 0, '队列=' + offline.queueSize())
t('该记录已到云端', (await serverGet('/records/today')).list.some(r => r.content === '监听触发记录'))

console.log('\n===== ' + pass + '/' + (pass + fail) + ' =====')
console.log('提示：测试账号 ' + phone + ' 可用 backend/scripts/clean-test-data.mjs 清理')
process.exit(fail === 0 ? 0 : 1)
