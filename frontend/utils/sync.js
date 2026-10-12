// 离线同步引擎
//
// 策略（策划书 §2.3「本地优先读写，网络恢复后自动与云端合并」）：
//   1. 写操作先落本地队列（保证不丢），再尝试直传。
//   2. 直传成功 -> 从队列摘除；网络失败 -> 留在队列等下次。
//   3. 服务端明确拒绝（skipped）-> 移出队列并记入 failed，
//      否则一条坏操作会堵死整条队列，后面的删除永远发不出去。
//   4. 触发时机：App 启动、回到前台、网络恢复、进入记录列表页。

import { syncApi } from './api.js'
import {
  getQueue,
  enqueue,
  removeByClientIds,
  markSynced,
  markSyncedNow,
  addFailed,
  setFailedReason,
  queueSize,
  offlineStatus
} from './offline.js'

const FLUSH_BATCH_SIZE = 50

let flushing = false
let networkBound = false

// 单次提交一批操作
// 返回 { ok, appliedIds, skipped }
//   ok=false 表示网络/服务异常：整批保留在队列里重试
//   skipped 为被服务端明确拒绝的条目（应移出队列并记入失败列表）
async function submitBatch(items) {
  try {
    const res = await syncApi.batch({ items })
    const applied = (res && res.applied) || []
    const skipped = (res && res.skipped) || []
    if (!Array.isArray(applied) && !Array.isArray(skipped)) {
      // 响应结构异常，按失败处理，保留队列
      return { ok: false, appliedIds: [], skipped: [] }
    }
    return {
      ok: true,
      appliedIds: applied.map((x) => x.clientId).filter(Boolean),
      skipped
    }
  } catch (e) {
    return { ok: false, appliedIds: [], skipped: [], error: e }
  }
}

// 把队列冲刷到服务端。返回 { flushed, failed, remaining }
export async function flushQueue() {
  if (flushing) return { flushed: 0, failed: 0, remaining: queueSize(), skipped: true }
  const queue = getQueue()
  if (!queue.length) return { flushed: 0, failed: 0, remaining: 0, skipped: false }

  flushing = true
  let flushed = 0
  let failed = 0
  try {
    for (let i = 0; i < queue.length; i += FLUSH_BATCH_SIZE) {
      const batch = queue.slice(i, i + FLUSH_BATCH_SIZE)
      const payload = batch.map((item) => ({
        clientId: item.clientId,
        op: item.op,
        type: item.type,
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        content: item.content,
        recordDate: item.recordDate,
        updatedAt: item.updatedAt,
        serverId: item.serverId
      }))
      const result = await submitBatch(payload)
      if (!result.ok) break // 网络或服务异常：整批保留，等下次

      // 成功的：移出队列并标记已同步
      if (result.appliedIds.length) {
        removeByClientIds(result.appliedIds)
        result.appliedIds.forEach((cid) => markSynced(cid))
        flushed += result.appliedIds.length
      }

      // 被拒绝的：移出队列（否则永久堵住后续操作），记入失败列表供用户查看
      if (result.skipped.length) {
        const rejectedIds = result.skipped.map((x) => x.clientId).filter(Boolean)
        if (rejectedIds.length) {
          rejectedIds.forEach((cid) => {
            const item = result.skipped.find((x) => x.clientId === cid)
            setFailedReason(cid, (item && item.reason) || '服务端拒绝')
          })
          addFailed(result.skipped
            .filter((x) => x.clientId)
            .map((x) => Object.assign({}, x, { at: new Date().toISOString() })))
          removeByClientIds(rejectedIds)
          failed += rejectedIds.length
        }
      }
    }
    if (flushed > 0) markSyncedNow()
  } catch (e) {
    // 静默：离线队列不能因异常而丢数据
  } finally {
    flushing = false
  }
  return { flushed, failed, remaining: queueSize(), skipped: false }
}

// 绑定网络恢复监听（只绑一次）
export function bindNetworkListener() {
  if (networkBound) return
  networkBound = true
  try {
    uni.onNetworkStatusChange((res) => {
      if (res && res.isConnected) {
        flushQueue()
      }
    })
  } catch (e) {
    // 部分平台不支持，忽略
  }
}

// App 启动 / 回前台时调用
export function syncOnResume() {
  bindNetworkListener()
  return flushQueue()
}

// 提交一条写操作：先入队，再尝试直传
// 入队保证「离线不丢」，直传成功则立即摘除并标记已同步。
// rejected 为被服务端拒绝的条目（例如分类无法解析），调用方可据此提示用户。
export async function submitOperation(op) {
  enqueue(op)
  const before = queueSize()
  const result = await flushQueue()
  const after = queueSize()
  return {
    queued: after > 0,
    flushed: result.flushed,
    failed: result.failed || 0,
    remaining: after,
    // 传进来的这条是否已被处理掉（成功或失败都算处理完）
    settled: after < before
  }
}

export { offlineStatus, queueSize }
