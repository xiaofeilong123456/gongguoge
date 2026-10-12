<template>
  <ThemePage>
    <!-- <view v-if="showLaunch" class="launch-overlay" @click="showLaunch = false">
      <view class="launch-ring">
        <text class="launch-brand">功过<br />格</text>
        <text class="launch-badge">自省</text>
      </view>
      <view class="launch-name">功过格</view>
      <view class="launch-motto">日日自省，徐徐而进</view>
      <view class="launch-enter">轻触进入</view>
    </view> -->

    <view class="today-head">
      <view>
        <view class="today-title">今日</view>
        <view class="date-line">{{ todayText }}</view>
        <view class="today-motto">日日自省，徐徐而进</view>
      </view>
      <view class="calendar-btn" @click="goCalendar">
        <GIcon name="calendar" :size="54" color="var(--ggg-terra)" />
      </view>
    </view>

    <view class="summary-card">
      <view class="summary-label">今日已记录 <text class="summary-number">{{ todayEntries.length }}</text> <text class="summary-unit">次</text></view>
      <view class="summary-grid">
        <view class="summary-item positive">
          <view class="type-icon positive">
            <view class="type-icon-inner"><GIcon name="check" :size="30" color="#ffffff" /></view>
          </view>
          <view>
            <view class="type-label positive-text">值得肯定</view>
            <view class="type-number">{{ positiveCount }}</view>
          </view>
        </view>
        <view class="summary-item reflect">
          <view class="type-icon reflect">
            <view class="type-icon-inner"><GIcon name="restart" :size="24" color="var(--ggg-terra-strong)" /></view>
          </view>
          <view>
            <view class="type-label reflect-text">有所觉察</view>
            <view class="type-number">{{ reflectCount }}</view>
          </view>
        </view>
      </view>
    </view>

    <view class="quick-record" @click="goRecord">
      <view class="quick-ring">
        <view class="quick-ring-inner"></view>
        <GIcon name="edit" :size="68" color="var(--ggg-accent-strong)" />
      </view>
      <view class="quick-title">快速记录</view>
      <view class="quick-subtitle">记下一次觉察</view>
    </view>

    <view class="list-head">
      <view class="list-title">今日列表</view>
      <view class="list-head-right">
        <view v-if="pendingCount > 0" class="sync-pill" @click="manualSync">
          <GIcon name="backup" :size="26" color="var(--ggg-accent)" />
          <text>{{ syncing ? '同步中…' : (pendingCount + ' 条待同步') }}</text>
        </view>
        <view class="list-count">{{ entries.length }} 条</view>
      </view>
    </view>

    <view class="ggg-card list-card" v-if="todayEntries.length">
      <view class="entry-row" v-for="item in todayEntries" :key="item.key">
        <view class="entry-icon" :class="item.type">
          <view class="entry-icon-inner" :class="item.type"><GIcon :name="item.type === 'positive' ? 'check' : 'restart'" :size="item.type === 'positive' ? 28 : 22" :color="item.type === 'positive' ? '#ffffff' : 'var(--ggg-terra-strong)'" /></view>
        </view>
        <view class="entry-main">
          <view class="entry-text">{{ item.text }}</view>
          <view class="entry-meta">{{ item.time }}<text v-if="item.pending" class="pending-badge">待同步</text></view>
        </view>
        <view class="entry-tag" :class="item.type">{{ item.tag }}</view>
        <view class="entry-delete" @click="deleteEntry(item)"><GIcon name="trash" :size="40" color="var(--ggg-text-3)" /></view>
      </view>
    </view>
    <view class="ggg-card empty-card" v-else>
      <view class="empty-title">今天还没有记录</view>
      <view class="ggg-muted ggg-small empty-sub">「今日列表」只显示今天的记录，过往记录在日历回望里查看</view>
      <view class="empty-link" @click="goCalendar">查看历史记录 ›</view>
    </view>

    <view class="ggg-card insight-card">
      <view class="insight-title">今日回望</view>
      <view class="insight-text">记录不构成分数、排名或道德评价，只用来帮助看见自己的行为节奏。</view>
    </view>

    <view v-if="undoVisible" class="undo-toast">
      <text>已删除记录，今日共 {{ todayEntries.length }} 次</text>
      <text class="undo-btn" @click="undoDelete">撤销</text>
    </view>

    <PageNav current="index" />
  </ThemePage>
</template>

<script>
import PageNav from '../../components/PageNav.vue'
import GIcon from '../../components/GIcon.vue'
import { recordApi } from '../../utils/api.js'
import { todayDate } from '../../utils/store.js'
import { getLocalRecords, upsertLocalRecord, removeLocalRecord, removeByClientId, enqueue } from '../../utils/offline.js'
import { syncOnResume, flushQueue } from '../../utils/sync.js'

export default {
  components: { PageNav, GIcon },
  data() {
    return {
      showLaunch: false,
      today: todayDate(),
      todayText: '',
      entries: [],
      localRecords: [],
      syncNotice: '',
      syncing: false,
      deletedItem: null,
      undoVisible: false,
      undoTimer: null
    }
  },
  computed: {
    // 服务端记录 + 本地待同步记录，按时间合并
    todayEntries() {
      const server = this.entries.filter(item => item.date === this.today)
      const local = this.localRecords
        // 只合并仍待同步的镜像记录；已同步的（pending=false）会作为服务端记录返回
        .filter(item => item.pending && !item.removed && item.recordDate === this.today)
        .map(item => ({
          key: item.clientId,
          id: null,
          clientId: item.clientId,
          type: item.type === 1 ? 'positive' : 'reflect',
          text: item.content,
          tag: item.tag || '',
          time: item.time || '',
          date: item.recordDate,
          pending: true,
          local: true
        }))
      return server.concat(local)
    },
    positiveCount() {
      return this.todayEntries.filter(item => item.type === 'positive').length
    },
    reflectCount() {
      return this.todayEntries.filter(item => item.type === 'reflect').length
    },
    pendingCount() {
      return this.todayEntries.filter(item => item.pending).length
    }
  },
  onLoad() {
    this.showLaunch = true
  },
  onShow() {
    this.today = todayDate()
    this.todayText = this.formatToday(new Date())
    // 1) 先用本地镜像立即渲染，不等网络
    this.refreshLocal()
    // 2) 同时拉取服务端数据（不依赖同步结果，避免被网络往返拖慢）
    this.loadToday()
    // 3) 后台补传离线队列；完成后再刷新一次
    this.syncNow()
  },
  methods: {
    formatToday(date) {
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日 ' + weekdays[date.getDay()]
    },
    refreshLocal() {
      this.localRecords = getLocalRecords()
    },
    // 后台补传离线队列；完成后刷新一次本地镜像与服务端数据
    // 注意：不在这里才首次加载列表 —— 队列为空是常态，
    // 那样会让列表等到同步往返结束才出现（录完记录后有明显延迟）。
    syncNow() {
      syncOnResume().then(() => {
        this.refreshLocal()
        this.loadToday()
      }).catch(() => {
        this.refreshLocal()
        this.loadToday()
      })
    },
    // 手动同步：用户主动点「待同步」时触发
    async manualSync() {
      if (this.syncing) return
      this.syncing = true
      try {
        const result = await flushQueue()
        this.refreshLocal()
        await this.loadToday()
        if (result && result.flushed > 0) {
          uni.showToast({ title: '同步完成', icon: 'none' })
        } else if (this.pendingCount > 0) {
          uni.showToast({ title: '同步未完成，请检查网络', icon: 'none' })
        } else {
          uni.showToast({ title: '已是最新', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '同步失败，请检查网络', icon: 'none' })
      } finally {
        this.syncing = false
      }
    },
    loadToday() {
      recordApi.today().then((res) => {
        this.entries = (res.list || []).map(this.normalizeRecord)
      }).catch(() => {
        // 网络不可用时保留服务端旧数据，本地待同步记录照常展示
      })
    },
    normalizeRecord(item) {
      return {
        key: 's_' + item.id,
        id: item.id,
        clientId: item.clientId || '',
        type: item.type === 1 ? 'positive' : 'reflect',
        text: item.content,
        tag: item.categoryName || '',
        time: item.createTime ? item.createTime.substring(11, 16) : '',
        date: item.recordDate,
        pending: false,
        local: false
      }
    },
    goRecord() {
      uni.navigateTo({ url: '/pages/record/record' })
    },
    goCalendar() {
      uni.navigateTo({ url: '/pages/calendar/calendar' })
    },
    deleteEntry(entry) {
      const index = this.entries.findIndex(item => item.key === entry.key)
      const snapshot = this.entries[index]

      // 1) 本地待同步记录：先从镜像里摘掉（不能删操作队列，队列里可能已有 create）
      if (entry.local) {
        const localSnapshot = this.localRecords.find(i => i.clientId === entry.clientId) || null
        removeLocalRecord(entry.clientId)
        this.refreshLocal()
        // 若该记录尚未同步，直接撤销队列里待提交的新增（不能改写成 delete）
        removeByClientId(entry.clientId)
        this.showUndo({ entry, index: -1, snapshot: localSnapshot, local: true })
        return
      }

      // 2) 服务端记录：先本地隐藏（保证离线可删），只入队不立即冲刷，
      //    给「撤销」留出 5 秒窗口；窗口结束后再提交，避免删了又走恢复流程。
      if (index >= 0) {
        this.entries.splice(index, 1)
      }
      const cid = entry.clientId || this.localIdOf(entry)
      // 带上 serverId：老数据可能没有 clientId，服务端需要靠它定位记录
      enqueue({ clientId: cid, op: 'delete', serverId: entry.id || null })
      this.showUndo({ entry, index, snapshot, local: false, clientId: cid })
    },
    // 服务端记录若没有 clientId，构造一个稳定的本地标识（仅用于队列去重）
    localIdOf(entry) {
      return 's_' + (entry.id || entry.key)
    },
    showUndo(payload) {
      this.deletedItem = payload
      this.undoVisible = true
      clearTimeout(this.undoTimer)
      this.undoTimer = setTimeout(() => {
        // 撤销窗口结束：把待提交的删除真正发出去
        if (this.deletedItem && !this.deletedItem.local) {
          flushQueue().catch(() => {})
        }
        this.undoVisible = false
        this.deletedItem = null
      }, 5000)
    },
    async undoDelete() {
      if (!this.deletedItem) return
      const { entry, index, snapshot, local, clientId } = this.deletedItem

      if (local) {
        // 未同步的本地记录：恢复镜像并重新提交新增
        upsertLocalRecord(snapshot || {
          clientId: entry.clientId,
          type: entry.type === 'positive' ? 1 : 2,
          content: entry.text,
          tag: entry.tag,
          recordDate: entry.date
        })
        this.refreshLocal()
        enqueue({
          clientId: entry.clientId,
          op: 'create',
          type: entry.type === 'positive' ? 1 : 2,
          content: entry.text,
          categoryName: entry.tag,
          recordDate: entry.date
        })
        flushQueue()
      } else {
        // 服务端记录：从队列移除刚入队的 delete，并恢复列表
        removeByClientId(clientId)
        if (index >= 0) {
          this.entries.splice(index, 0, snapshot)
        }
        if (entry.id) {
          recordApi.restore(entry.id).catch(() => {
            uni.showToast({ title: '撤销失败，请稍后重试', icon: 'none' })
          })
        }
      }

      this.undoVisible = false
      this.deletedItem = null
    }
  },
  onUnload() {
    clearTimeout(this.undoTimer)
  }
}
</script>

<style scoped>
.launch-overlay {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 99;
  background: var(--ggg-page-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.launch-ring {
  position: relative;
  width: 288rpx;
  height: 288rpx;
  border: 36rpx solid rgba(40, 43, 38, 0.16);
  border-radius: 50%;
  box-shadow: inset 4rpx 0 0 rgba(40, 43, 38, 0.13), 0 0 0 6rpx rgba(40, 43, 38, 0.035);
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-4deg);
}

.launch-brand {
  font-size: 64rpx;
  font-weight: 900;
  line-height: 1.25;
  letter-spacing: 8rpx;
  margin-left: 8rpx;
}

.launch-badge {
  position: absolute;
  right: 24rpx;
  bottom: 40rpx;
  background: var(--ggg-terra-deep);
  color: #ffffff;
  font-size: 20rpx;
  padding: 8rpx 12rpx;
  border-radius: 8rpx;
}

.launch-name {
  margin-top: 64rpx;
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: 8rpx;
}

.launch-motto {
  margin-top: 24rpx;
  color: #6e706b;
  font-size: 28rpx;
  letter-spacing: 8rpx;
}

.launch-enter {
  position: absolute;
  bottom: 128rpx;
  color: var(--ggg-accent);
  font-size: 28rpx;
}

.today-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  /* 给小程序自带的右上角胶囊按钮留出空间，避免日历图标与其重叠 */
  padding-top: 12rpx;
  padding-right: 12rpx;
}

.today-title {
  font-size: 56rpx;
  font-weight: 700;
}

.date-line {
  margin-top: 8rpx;
  color: var(--ggg-text-1);
  font-size: 28rpx;
  font-weight: 600;
}

.today-motto {
  margin-top: 16rpx;
  color: var(--ggg-text-2);
  font-size: 28rpx;
  letter-spacing: 4rpx;
}

.calendar-btn {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ggg-terra);
}

.summary-card {
  margin-top: 40rpx;
  background: var(--ggg-surface-card);
  border: 2rpx solid var(--ggg-border-3);
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.summary-label {
  font-weight: 600;
  font-size: 28rpx;
}

.summary-number {
  font-size: 48rpx;
  margin-left: 8rpx;
  font-variant-numeric: tabular-nums;
}

.summary-unit {
  font-size: 24rpx;
  color: #88877f;
}

.summary-grid {
  display: flex;
  margin-top: 24rpx;
  border-top: 0;
}

.summary-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.summary-item.positive {
  border-right: 2rpx solid #e5e1d9;
  padding-right: 24rpx;
}

.summary-item.reflect {
  padding-left: 32rpx;
}

.type-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.type-icon.positive {
  background: var(--ggg-accent-soft2);
}

.type-icon.reflect {
  background: var(--ggg-terra-soft);
}

.type-icon-inner {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-icon.positive .type-icon-inner {
  background: var(--ggg-green-fill);
}

.type-icon.reflect .type-icon-inner {
  border: 6rpx solid var(--ggg-terra-strong);
}

.positive-text {
  color: var(--ggg-accent-strong);
}

.reflect-text {
  color: var(--ggg-terra);
}

.type-label {
  font-size: 24rpx;
  font-weight: 600;
}

.type-number {
  font-size: 40rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.quick-record {
  margin-top: 16rpx;
  min-height: 276rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.quick-ring {
  position: relative;
  width: 168rpx;
  height: 168rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-ring-inner {
  position: absolute;
  left: 16rpx;
  top: 16rpx;
  right: 16rpx;
  bottom: 16rpx;
  border: 18rpx solid rgba(40, 43, 38, 0.16);
  border-radius: 50%;
  box-shadow: inset 4rpx 0 0 rgba(40, 43, 38, 0.13), 0 0 0 6rpx rgba(40, 43, 38, 0.035);
  transform: rotate(-4deg);
}

.quick-ring .g-icon {
  position: relative;
}

.quick-title {
  margin-top: -8rpx;
  font-weight: 700;
  font-size: 32rpx;
}

.quick-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--ggg-text-2);
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 32rpx 4rpx 16rpx;
}

.list-title {
  font-weight: 700;
  font-size: 32rpx;
}

.list-count {
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.list-head-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.sync-pill {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
  font-size: 22rpx;
  font-weight: 600;
}

.list-card {
  padding: 0;
  overflow: hidden;
}

.entry-row {
  min-height: 132rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 2rpx solid var(--ggg-border);
}

.entry-row:last-child {
  border-bottom: none;
}

.entry-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.entry-icon.positive {
  background: var(--ggg-accent-soft2);
}

.entry-icon.reflect {
  background: var(--ggg-terra-soft);
}

.entry-icon-inner {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.entry-icon-inner.positive {
  background: var(--ggg-green-fill);
}

.entry-icon-inner.reflect {
  border: 6rpx solid var(--ggg-terra-strong);
}

.entry-main {
  flex: 1;
  min-width: 0;
}

.entry-text {
  font-size: 28rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-meta {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--ggg-text-3b);
  font-variant-numeric: tabular-nums;
}

.pending-badge {
  margin-left: 12rpx;
  padding: 2rpx 10rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  background: var(--ggg-terra-soft);
  color: var(--ggg-error-text);
}

.entry-tag {
  flex: none;
  font-size: 22rpx;
  padding: 8rpx 16rpx;
  border-radius: 16rpx;
}

.entry-tag.positive {
  background: var(--ggg-accent-softer);
  color: var(--ggg-tag-positive-text);
}

.entry-tag.reflect {
  background: var(--ggg-terra-soft2);
  color: var(--ggg-error-text);
}

.entry-delete {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ggg-text-3);
}

.empty-card {
  text-align: center;
  color: var(--ggg-text-3);
  font-size: 28rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--ggg-text-1);
}

.empty-sub {
  margin-top: 10rpx;
  line-height: 1.6;
}

.empty-link {
  margin-top: 18rpx;
  color: var(--ggg-accent);
  font-size: 26rpx;
  font-weight: 600;
}

.insight-card {
  margin-top: 20rpx;
  background: var(--ggg-surface-card-soft);
}

.insight-title {
  font-weight: 700;
  font-size: 32rpx;
}

.insight-text {
  margin-top: 12rpx;
  color: var(--ggg-text-2);
  font-size: 28rpx;
  line-height: 1.6;
}

.undo-toast {
  position: fixed;
  left: 32rpx;
  right: 32rpx;
  bottom: 140rpx;
  z-index: 50;
  min-height: 88rpx;
  box-sizing: border-box;
  border-radius: 24rpx;
  background: #2f302e;
  color: #ffffff;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 28rpx;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.18);
}

.undo-btn {
  color: #e0a05b;
  font-weight: 600;
  padding: 16rpx 8rpx;
}
</style>
