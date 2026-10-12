// 离线优先：本地操作队列与待同步记录
//
// 设计目标（策划书 §2.2 / §2.3）：
//   - 无网络时记录先落本地，不报错、不丢失；
//   - 恢复网络后自动与云端合并；
//   - 重复提交由服务端按 clientId 幂等去重（见 RecordService）。
//
// 操作队列记录「用户意图」而不是「请求」：
//   create 新增、update 修改、delete 删除（进回收站）、purge 彻底删除。
// 同一 clientId 的多次操作会依次重放，服务端按顺序应用。

const QUEUE_KEY = 'ggg_op_queue'
const LOCAL_RECORDS_KEY = 'ggg_local_records'
const MAX_LOCAL_RECORDS = 300

// ---------- 用户作用域 ----------
//
// 离线队列与本地镜像必须按用户隔离：不同账号登录同一设备，
// 绝不能看到彼此的待同步记录。用当前登录用户 ID 作为存储 key 后缀。

const SCOPE_KEY = 'ggg_offline_scope'

export function setOfflineScope(userId) {
  if (userId === undefined || userId === null || userId === '') {
    uni.removeStorageSync(SCOPE_KEY)
    return
  }
  uni.setStorageSync(SCOPE_KEY, String(userId))
}

function scope() {
  try {
    const s = uni.getStorageSync(SCOPE_KEY)
    return s ? 'u' + s : 'anon'
  } catch (e) {
    return 'anon'
  }
}

function queueKey() {
  return QUEUE_KEY + '_' + scope()
}

function localRecordsKey() {
  return LOCAL_RECORDS_KEY + '_' + scope()
}

// 清空当前作用域下的所有离线数据（登出时调用，避免残留）
export function clearOfflineScope() {
  uni.removeStorageSync(queueKey())
  uni.removeStorageSync(localRecordsKey())
  uni.removeStorageSync('ggg_sync_failed')
}

// ---------- 唯一 ID ----------

// 生成稳定的客户端记录 ID（比纯时间戳更不易碰撞）
export function newClientId() {
  const rand = Math.random().toString(36).slice(2, 10)
  return 'c_' + Date.now().toString(36) + '_' + rand
}

// ---------- 队列读写 ----------

export function getQueue() {
  try {
    const list = uni.getStorageSync(queueKey())
    return Array.isArray(list) ? list : []
  } catch (e) {
    return []
  }
}

function saveQueue(list) {
  uni.setStorageSync(queueKey(), Array.isArray(list) ? list : [])
}

export function queueSize() {
  return getQueue().length
}

export function hasPending() {
  return queueSize() > 0
}

// 入队。同一 clientId 的重复操作会合并：
//   - 已存在 create 且又来 create：保留最早那条（避免重复新增）
//   - update 覆盖同 clientId 的旧 update
//   - delete / purge 会清掉同 clientId 的 update（没必要改一条即将删除的记录）
export function enqueue(op) {
  const queue = getQueue()
  const same = queue.filter((item) => item.clientId === op.clientId)

  if (op.op === 'create' && same.some((item) => item.op === 'create')) {
    return queue // 已经排了一个新增，不重复
  }

  if (op.op === 'update') {
    const existUpdate = queue.findIndex((item) => item.clientId === op.clientId && item.op === 'update')
    if (existUpdate >= 0) {
      queue[existUpdate] = op
      saveQueue(queue)
      return queue
    }
  }

  if (op.op === 'delete' || op.op === 'purge') {
    const filtered = queue.filter((item) => !(item.clientId === op.clientId && item.op === 'update'))
    filtered.push(op)
    saveQueue(filtered)
    return filtered
  }

  queue.push(op)
  saveQueue(queue)
  return queue
}

// 按 clientId 集合从队列移除（用于批量提交成功后清理）
export function removeByClientIds(ids) {
  const set = new Set(ids)
  const rest = getQueue().filter((item) => !set.has(item.clientId))
  saveQueue(rest)
  return rest
}

// 精确移除某条记录在队列中的所有操作。
// 用于「本地新增后、尚未同步就删除」的场景：此时直接撤销待提交的新增即可，
// 不能改成 delete 操作（否则服务端若已存在会误删）。
export function removeByClientId(clientId) {
  const rest = getQueue().filter((item) => item.clientId !== clientId)
  saveQueue(rest)
  return rest
}

export function clearQueue() {
  saveQueue([])
}

// ---------- 本地待同步记录（供 UI 展示） ----------

export function getLocalRecords() {
  try {
    const list = uni.getStorageSync(localRecordsKey())
    return Array.isArray(list) ? list : []
  } catch (e) {
    return []
  }
}

function saveLocalRecords(list) {
  const trimmed = list.slice(-MAX_LOCAL_RECORDS)
  uni.setStorageSync(localRecordsKey(), trimmed)
}

// 新增/更新本地镜像记录（pending=true 表示尚未确认同步）
export function upsertLocalRecord(record) {
  const list = getLocalRecords()
  const idx = list.findIndex((item) => item.clientId === record.clientId)
  const next = Object.assign({ pending: true }, record)
  if (idx >= 0) {
    list[idx] = Object.assign({}, list[idx], next)
  } else {
    list.push(next)
  }
  saveLocalRecords(list)
  return list
}

// 标记某条本地记录已确认（服务端已存在）；保留记录但去掉 pending
export function markSynced(clientId, serverId) {
  const list = getLocalRecords()
  const idx = list.findIndex((item) => item.clientId === clientId)
  if (idx >= 0) {
    list[idx] = Object.assign({}, list[idx], { pending: false, serverId: serverId || list[idx].serverId || null })
    saveLocalRecords(list)
  }
  return list
}

export function removeLocalRecord(clientId) {
  const list = getLocalRecords().filter((item) => item.clientId !== clientId)
  saveLocalRecords(list)
  return list
}

export function clearLocalRecords() {
  saveLocalRecords([])
}

// 清理已同步完成的镜像记录（保留仍 pending 的）
export function pruneSyncedLocalRecords() {
  const list = getLocalRecords().filter((item) => item.pending)
  saveLocalRecords(list)
  return list
}

// ---------- 同步失败记录（服务端明确拒绝的操作） ----------
//
// 服务端拒绝（例如分类无法解析）时，操作会从队列移出，但绝不能静默丢弃：
// 这里保留一份记录，供设置页/调试查看，并支持重新加入队列重试。

const FAILED_KEY = 'ggg_sync_failed'
const FAILED_MAX = 50

export function getFailed() {
  try {
    const list = uni.getStorageSync(FAILED_KEY)
    return Array.isArray(list) ? list : []
  } catch (e) {
    return []
  }
}

export function addFailed(entries) {
  if (!entries || !entries.length) return getFailed()
  const list = getFailed().concat(entries).slice(-FAILED_MAX)
  uni.setStorageSync(FAILED_KEY, list)
  return list
}

// 记录失败原因（把用户可读的原因挂到对应 clientId 上）
export function setFailedReason(clientId, reason) {
  if (!clientId) return
  const list = getFailed()
  const idx = list.findIndex((x) => x.clientId === clientId)
  if (idx >= 0) {
    list[idx] = Object.assign({}, list[idx], { reason: reason })
  } else {
    list.push({ clientId, reason, at: new Date().toISOString() })
  }
  uni.setStorageSync(FAILED_KEY, list.slice(-FAILED_MAX))
}

export function clearFailed() {
  uni.setStorageSync(FAILED_KEY, [])
}

// ---------- 状态汇总（设置页展示用） ----------

export function offlineStatus() {
  const queue = getQueue()
  const pendingRecords = getLocalRecords().filter((item) => item.pending)
  return {
    queueSize: queue.length,
    pendingRecords: pendingRecords.length,
    failedCount: getFailed().length,
    lastSyncAt: (() => {
      try {
        return uni.getStorageSync('ggg_last_sync_at') || ''
      } catch (e) {
        return ''
      }
    })()
  }
}

export function markSyncedNow() {
  uni.setStorageSync('ggg_last_sync_at', new Date().toISOString())
}
