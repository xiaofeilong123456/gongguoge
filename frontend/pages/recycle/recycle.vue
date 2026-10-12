<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">回收站</view>
      <view class="ggg-back"><GIcon name="recycle" :size="30" color="var(--ggg-terra)" /></view>
    </view>

    <view class="ggg-muted ggg-small">删除记录保留 7 天，可恢复或彻底删除</view>
    <view class="ggg-card list-card" v-if="items.length">
      <view v-for="item in items" :key="item.id" class="recycle-row">
        <view class="recycle-head">
          <view class="recycle-mark" :class="item.type === 1 ? 'positive' : 'reflect'"><GIcon :name="item.type === 1 ? 'check' : 'restart'" :size="24" :color="item.type === 1 ? 'var(--ggg-accent)' : 'var(--ggg-terra)'" /></view>
          <view class="recycle-main">
            <view class="recycle-text">{{ item.text }}</view>
            <view class="recycle-meta">{{ item.tag }} · 已删除 · 可恢复</view>
          </view>
        </view>
        <view class="recycle-actions">
          <view class="restore-btn" @click="restore(item.id)"><GIcon name="recycle" :size="24" color="var(--ggg-accent)" /> 恢复</view>
          <view class="permanent-btn" @click="removePermanent(item.id)"><GIcon name="trash" :size="24" color="var(--ggg-terra)" /> 彻底删除</view>
        </view>
      </view>
    </view>
    <view class="ggg-card empty-card" v-else>回收站是空的</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { recordApi } from '../../utils/api.js'
import { submitOperation } from '../../utils/sync.js'

export default {
  components: { GIcon },
  data() {
    return {
      items: []
    }
  },
  onShow() {
    this.loadItems()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    loadItems() {
      recordApi.recycle().then((list) => {
        this.items = (list || []).map(item => ({
          id: item.id,
          clientId: item.clientId || '',
          type: item.type,
          text: item.content,
          tag: item.categoryName || '',
          deletedAt: '',
          daysLeft: 7
        }))
      }).catch(() => {
        this.items = []
      })
    },
    restore(id) {
      recordApi.restore(id).then(() => {
        this.items = this.items.filter(entry => entry.id !== id)
        uni.showToast({ title: '已恢复', icon: 'success' })
      }).catch((err) => {
        uni.showToast({ title: (err && err.message) || '恢复失败', icon: 'none' })
      })
    },
    removePermanent(id) {
      const item = this.items.find(entry => entry.id === id)
      uni.showModal({
        title: '彻底删除',
        content: '彻底删除后无法恢复，确认继续？',
        success: (res) => {
          if (!res.confirm) return
          // 走离线队列：保证离线状态下也能彻底删除，联网后补传
          const op = {
            clientId: (item && item.clientId) || ('s_' + id),
            op: 'purge',
            serverId: id
          }
          submitOperation(op).then(() => {
            this.items = this.items.filter(entry => entry.id !== id)
            uni.showToast({ title: '已彻底删除', icon: 'none' })
          }).catch(() => {
            uni.showToast({ title: '删除失败，请稍后重试', icon: 'none' })
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.list-card {
  margin-top: 24rpx;
  padding: 0;
  overflow: hidden;
}

.recycle-row {
  padding: 32rpx;
  border-bottom: 2rpx solid var(--ggg-border);
}

.recycle-row:last-child {
  border-bottom: none;
}

.recycle-head {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.recycle-mark {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.recycle-mark.positive {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
}

.recycle-mark.reflect {
  background: var(--ggg-terra-soft);
  color: var(--ggg-terra);
}

.recycle-main {
  flex: 1;
  min-width: 0;
}

.recycle-text {
  font-size: 28rpx;
  font-weight: 600;
}

.recycle-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.recycle-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 24rpx;
}

.restore-btn,
.permanent-btn {
  flex: 1;
  height: 80rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
  gap: 8rpx;
}

.restore-btn {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
}

.permanent-btn {
  background: var(--ggg-terra-soft);
  color: var(--ggg-terra);
}

.empty-card {
  margin-top: 24rpx;
  text-align: center;
  color: var(--ggg-text-3);
}
</style>
