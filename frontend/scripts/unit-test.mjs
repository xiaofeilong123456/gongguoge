// 前端工具模块单元测试
//
// 用法:
//   node frontend/scripts/unit-test.mjs
//
// 通过 mock 的 uni / plus 全局对象，验证纯逻辑模块的实际行为，
// 不需要 HBuilderX，也不需要运行中的数据服务。只依赖 Node 18+。

import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const utils = path.resolve(here, '..', 'utils')

// ---- mock uni 存储 ----
const store = new Map()
globalThis.uni = {
  getStorageSync: (k) => (store.has(k) ? store.get(k) : ''),
  setStorageSync: (k, v) => store.set(k, v),
  removeStorageSync: (k) => store.delete(k),
  onNetworkStatusChange: () => {}
}

let pass = 0
let fail = 0
function t(name, cond, detail) {
  if (cond) {
    pass++
    console.log('PASS  ' + name)
  } else {
    fail++
    console.log('FAIL  ' + name + (detail ? '  -> ' + detail : ''))
  }
}

// ================= privacy.js =================
const {
  PRIVACY_VERSION,
  getConsent,
  hasAgreedPrivacy,
  agreePrivacy,
  resetPrivacy,
  formatAgreedAt
} = await import('file://' + path.join(utils, 'privacy.js').replace(/\\/g, '/'))

t('初始状态：未同意协议', hasAgreedPrivacy() === false)
const c0 = getConsent()
t('未同意时 getConsent 返回空版本', c0.version === '' && c0.agreedAt === '', JSON.stringify(c0))

agreePrivacy()
t('同意后 hasAgreedPrivacy 为 true', hasAgreedPrivacy() === true)
const c1 = getConsent()
t('同意记录包含当前版本', c1.version === PRIVACY_VERSION, 'version=' + c1.version)
t('同意记录包含合法时间戳', !!c1.agreedAt && !isNaN(new Date(c1.agreedAt).getTime()), 'agreedAt=' + c1.agreedAt)

// 版本升级后应重新要求同意
store.set('ggg_privacy_consent', { version: '0.9', agreedAt: '2026-01-01T00:00:00.000Z' })
t('旧版本同意记录不被视为已同意', hasAgreedPrivacy() === false)

agreePrivacy()
t('重新同意后通过', hasAgreedPrivacy() === true)
resetPrivacy()
t('撤回后回到未同意状态', hasAgreedPrivacy() === false)
t('撤回后存储被清空', getConsent().version === '')

const fmt = formatAgreedAt('2026-10-07T15:30:00.000Z')
t('formatAgreedAt 返回可读字符串', /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(fmt), fmt)
t('formatAgreedAt 对空值返回空串', formatAgreedAt('') === '')

// ================= platform.js =================
const { isApp, canLocalNotify, quitApp } =
  await import('file://' + path.join(utils, 'platform.js').replace(/\\/g, '/'))

t('无 plus 时 isApp 为 false', isApp() === false)
t('无 plus 时 canLocalNotify 为 false', canLocalNotify() === false)
t('无 plus 时 quitApp 返回 false 而不抛错', quitApp() === false)

// 模拟 App 运行环境
globalThis.plus = {
  runtime: { quit: () => { globalThis.__quitCalled = true } },
  push: { createMessage: () => {} }
}
t('有 plus 时 isApp 为 true', isApp() === true)
t('有 plus.push 时 canLocalNotify 为 true', canLocalNotify() === true)
t('有 plus 时 quitApp 调用 runtime.quit', quitApp() === true && globalThis.__quitCalled === true)
delete globalThis.plus

// ================= store.js（不依赖假数据）=================
const { formatDate, todayDate, maskPhone, getProfile, saveProfile } =
  await import('file://' + path.join(utils, 'store.js').replace(/\\/g, '/'))

t('formatDate 补零正确', formatDate(new Date(2026, 0, 5)) === '2026-01-05', formatDate(new Date(2026, 0, 5)))
t('todayDate 与 formatDate 一致', todayDate() === formatDate(new Date()), todayDate())
t('maskPhone 掩码 11 位手机号', maskPhone('13812345678') === '138****5678', maskPhone('13812345678'))
t('maskPhone 对短串原样返回', maskPhone('123') === '123', maskPhone('123'))
t('maskPhone 对空值安全', maskPhone('') === '' && maskPhone(undefined) === '')

store.clear()
const p0 = getProfile()
t('无缓存时 getProfile 返回占位且不含假手机号', p0.nickname === '功过格用户' && p0.phone === '', JSON.stringify(p0))
saveProfile({ nickname: '测试昵称', phone: '138****0000', avatar: 'a.png' })
const p1 = getProfile()
t('saveProfile 后 getProfile 一致', p1.nickname === '测试昵称' && p1.phone === '138****0000', JSON.stringify(p1))
saveProfile(null)
t('saveProfile 对 null 安全', getProfile().nickname === '功过格用户')

// ================= offline.js（离线队列）=================
const {
  newClientId,
  getQueue,
  enqueue,
  queueSize,
  removeByClientIds,
  removeByClientId,
  clearQueue,
  getLocalRecords,
  upsertLocalRecord,
  removeLocalRecord,
  clearLocalRecords,
  pruneSyncedLocalRecords
} = await import('file://' + path.join(utils, 'offline.js').replace(/\\/g, '/'))

clearQueue()
clearLocalRecords()

const idA = newClientId()
t('newClientId 唯一且非空', !!idA && idA !== newClientId(), idA)

enqueue({ clientId: idA, op: 'create', type: 1, content: 'A' })
t('入队后队列长度正确', queueSize() === 1, 'size=' + queueSize())

// 重复 create 合并为一条
enqueue({ clientId: idA, op: 'create', type: 1, content: 'A' })
t('重复 create 不重复入队', queueSize() === 1, 'size=' + queueSize())

// update 同一 clientId 覆盖旧 update
enqueue({ clientId: idA, op: 'update', content: 'A2' })
enqueue({ clientId: idA, op: 'update', content: 'A3' })
const updItems = getQueue().filter((x) => x.clientId === idA && x.op === 'update')
t('同 clientId 的 update 只保留最新一条', updItems.length === 1 && updItems[0].content === 'A3',
  JSON.stringify(updItems))

// delete 会清掉同 clientId 的 update
enqueue({ clientId: idA, op: 'delete' })
const afterDel = getQueue().filter((x) => x.clientId === idA)
t('delete 入队后清除该记录的 update', afterDel.every((x) => x.op !== 'update'), JSON.stringify(afterDel))

// 精确移除
enqueue({ clientId: idA, op: 'create' })
removeByClientId(idA)
t('removeByClientId 清空该记录的全部操作', getQueue().filter((x) => x.clientId === idA).length === 0,
  JSON.stringify(getQueue()))

// 批量移除
const idB = newClientId(), idC = newClientId()
enqueue({ clientId: idB, op: 'create', content: 'B' })
enqueue({ clientId: idC, op: 'create', content: 'C' })
removeByClientIds([idB, idC])
t('removeByClientIds 批量清理', queueSize() === 0, 'size=' + queueSize())

// 本地镜像
upsertLocalRecord({ clientId: idA, type: 1, content: 'A', recordDate: '2026-10-07' })
upsertLocalRecord({ clientId: idA, type: 1, content: 'A改', recordDate: '2026-10-07' })
t('upsertLocalRecord 覆盖同 clientId 记录', getLocalRecords().length === 1 && getLocalRecords()[0].content === 'A改',
  JSON.stringify(getLocalRecords()))
t('本地记录默认标记 pending', getLocalRecords()[0].pending === true)

// 标记已同步后 prune 只保留 pending
const { markSynced } = await import('file://' + path.join(utils, 'offline.js').replace(/\\/g, '/'))
markSynced(idA, 999)
t('markSynced 清除 pending 并记录 serverId',
  getLocalRecords()[0].pending === false && getLocalRecords()[0].serverId === 999,
  JSON.stringify(getLocalRecords()[0]))
pruneSyncedLocalRecords()
t('pruneSyncedLocalRecords 移除已同步条目', getLocalRecords().length === 0, '剩 ' + getLocalRecords().length)

removeLocalRecord(idA)
t('removeLocalRecord 幂等（不存在也不报错）', getLocalRecords().length === 0)

// ================= permission.js（权限说明）=================
// mock showModal：可控制用户点确认还是取消
let modalResponse = { confirm: true }
let modalCalls = []
globalThis.uni.showModal = (opts) => {
  modalCalls.push(opts)
  if (opts.success) opts.success(modalResponse)
}

const {
  PERMISSION_COPY,
  hasShownPermission,
  markPermissionShown,
  resetPermissionShown,
  explainPermission,
  permissionOverview
} = await import('file://' + path.join(utils, 'permission.js').replace(/\\/g, '/'))

resetPermissionShown()
t('初始状态：未说明过任何权限', Object.keys(PERMISSION_COPY).every((k) => !hasShownPermission(k)))
t('permissionOverview 覆盖全部权限且初始为未说明', permissionOverview().length === Object.keys(PERMISSION_COPY).length &&
  permissionOverview().every((x) => x.explainedAt === ''), JSON.stringify(permissionOverview().map((x) => x.explainedAt)))

// 用户同意
modalResponse = { confirm: true }
modalCalls = []
const agreed = await explainPermission('notification')
t('用户点同意 -> 返回 true', agreed === true)
t('说明弹窗真的弹出过一次', modalCalls.length === 1, 'calls=' + modalCalls.length)
const noticeCall = modalCalls[0] || {}
t('弹窗文案包含用途说明', /提醒你完成当天的复盘记录/.test(noticeCall.content || ''),
  (noticeCall.content || '').slice(0, 60))
t('弹窗文案包含拒绝后果', /仍可正常记录/.test(noticeCall.content || ''))
t('同意后记录为已说明', hasShownPermission('notification') === true)
t('已说明的权限在概览中带时间', !!permissionOverview().find((x) => x.key === 'notification').explainedAt)

// 用户拒绝
resetPermissionShown()
modalResponse = { confirm: false }
const refused = await explainPermission('notification')
t('用户点取消 -> 返回 false', refused === false)
t('拒绝后仍记录为已说明（避免反复打扰）', hasShownPermission('notification') === true)

// markPermissionShown 幂等且可重置
markPermissionShown('storage')
t('markPermissionShown 生效', hasShownPermission('storage') === true)
resetPermissionShown()
t('resetPermissionShown 清空全部', !hasShownPermission('notification') && !hasShownPermission('storage'))

// 未知权限键不应误报成功
const unknown = await explainPermission('not_a_permission')
t('未知权限键返回 false', unknown === false)

// 导出说明按平台给出不同文案（Node 环境下 window/document 不存在 -> 视为非 H5）
modalCalls = []
await explainPermission('export')
t('导出说明含剪贴板说明（非 H5 环境）', /剪贴板/.test((modalCalls[0] || {}).content || ''),
  (( modalCalls[0] || {}).content || '').slice(0, 60))

resetPermissionShown()

console.log('\n===== ' + pass + '/' + (pass + fail) + ' 通过 =====')
process.exit(fail === 0 ? 0 : 1)
