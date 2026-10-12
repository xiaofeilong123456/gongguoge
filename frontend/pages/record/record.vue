<template>
  <ThemePage class="record-page">
    <view class="sheet-title">记下一笔</view>
    <view class="sheet-subtitle">写下一次觉察即可</view>

    <view class="type-grid">
      <view class="type-card" :class="{ active: type === 'positive' }" @click="type = 'positive'">
        <view class="type-mark positive"><GIcon name="check" :size="34" color="#ffffff" /></view>
        <view>值得肯定</view>
      </view>
      <view class="type-card" :class="{ active: type === 'reflect' }" @click="type = 'reflect'">
        <view class="type-mark reflect"><GIcon name="restart" :size="26" color="var(--ggg-terra-strong)" /></view>
        <view>有所觉察</view>
      </view>
    </view>

    <view class="ggg-card form-card">
      <view class="form-label">发生了什么？</view>
      <textarea class="ggg-textarea" v-model="text" placeholder="写下一句话即可……"></textarea>

      <view class="form-label">标签 <text class="ggg-muted">（选填）</text></view>
      <view class="tag-wrap">
        <view
          v-for="tag in availableTags"
          :key="tag"
          class="tag-item"
          :class="{ active: tag === selectedTag }"
          @click="selectedTag = tag"
        >{{ tag }}</view>
      </view>

      <view class="form-label">日期</view>
      <picker mode="date" :value="recordDate" :end="maxDate" @change="onDateChange">
        <view class="date-picker">
          <text class="date-text">{{ dateLabel }} · {{ recordDate }}</text>
          <GIcon name="calendar" :size="30" color="var(--ggg-accent)" />
        </view>
      </picker>
      <view class="ggg-muted date-hint">可补填过往日期；未来日期不可选</view>

      <view v-if="!canSave" class="required-hint">请先写下这次记录的内容</view>
    </view>

    <view class="action-bar">
      <view class="ggg-btn cancel-btn" @click="goBack">取消</view>
      <view class="ggg-btn ggg-btn-primary save-btn" :class="{ disabled: !canSave }" @click="save">记录</view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { categoryApi } from '../../utils/api.js'
import { todayDate } from '../../utils/store.js'
import { newClientId, upsertLocalRecord, enqueue } from '../../utils/offline.js'
import { flushQueue } from '../../utils/sync.js'

export default {
  components: { GIcon },
  data() {
    return {
      type: 'positive',
      text: '',
      categories: [],
      selectedTag: '习惯',
      recordDate: todayDate(),
      saving: false
    }
  },
  computed: {
    availableTags() {
      const tags = this.categories.filter(item => item.type === (this.type === 'positive' ? 1 : 2)).map(item => item.name)
      return tags.length ? tags : ['习惯']
    },
    canSave() {
      return this.text.trim().length > 0
    },
    // 未来日期不可选
    maxDate() {
      return todayDate()
    },
    dateLabel() {
      const today = todayDate()
      if (this.recordDate === today) return '今天'
      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      const y = yesterday.getFullYear() + '-' + String(yesterday.getMonth() + 1).padStart(2, '0') + '-' + String(yesterday.getDate()).padStart(2, '0')
      if (this.recordDate === y) return '昨天'
      return '补记'
    }
  },
  onShow() {
    this.loadCategories()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    onDateChange(e) {
      this.recordDate = e.detail.value
    },
    loadCategories() {
      categoryApi.list().then((list) => {
        this.categories = list || []
        if (this.availableTags.indexOf(this.selectedTag) < 0) {
          this.selectedTag = this.availableTags[0]
        }
      }).catch(() => {
        this.categories = []
      })
    },
    save() {
      if (!this.canSave || this.saving) return
      const typeNumber = this.type === 'positive' ? 1 : 2
      const category = this.categories.find(item => item.type === typeNumber && item.name === this.selectedTag)
      const content = this.text.trim()
      this.saving = true

      const clientId = newClientId()
      const op = {
        clientId,
        op: 'create',
        type: typeNumber,
        categoryId: category ? category.id : null,
        categoryName: this.selectedTag,
        content,
        recordDate: this.recordDate
      }

      // 1) 本地镜像 + 入队：这两步都是本地操作，立即完成
      upsertLocalRecord({
        clientId,
        type: typeNumber,
        content,
        tag: this.selectedTag,
        recordDate: this.recordDate,
        time: new Date().toTimeString().slice(0, 5)
      })
      enqueue(op)

      // 2) 立刻返回首页：首页会用本地镜像立刻显示这条记录，
      //    不必等网络往返（之前 await 同步完成才返回，能感觉到延迟）
      uni.showToast({ title: '已记录', icon: 'success', duration: 600 })
      this.saving = false
      setTimeout(() => uni.navigateBack(), 150)

      // 3) 后台提交；失败会留在队列里，由首页的「待同步」入口补传
      flushQueue().catch(() => {})
    }
  }
}
</script>

<style scoped>
.record-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 220rpx;
}

.sheet-handle {
  width: 80rpx;
  height: 8rpx;
  border-radius: 8rpx;
  background: var(--ggg-border-strong);
  margin: 8rpx auto 24rpx;
}

.sheet-title {
  text-align: center;
  font-size: 36rpx;
  font-weight: 700;
}

.sheet-subtitle {
  text-align: center;
  font-size: 24rpx;
  color: #88877f;
  margin-top: 8rpx;
}

.type-grid {
  display: flex;
  gap: 24rpx;
  margin-top: 32rpx;
}

.type-card {
  flex: 1;
  min-height: 152rpx;
  border-radius: 32rpx;
  background: var(--ggg-surface);
  border: 4rpx solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  color: #9b5630;
  font-weight: 600;
  font-size: 28rpx;
}

.type-card.active {
  background: var(--ggg-accent-soft);
  border-color: var(--ggg-green-fill);
  color: var(--ggg-accent);
}

.type-mark {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.type-mark.positive {
  background: var(--ggg-green-fill);
  color: #ffffff;
}

.type-mark.reflect {
  border: 6rpx solid var(--ggg-terra-strong);
  color: var(--ggg-terra-strong);
  background: var(--ggg-surface);
}

.form-card {
  margin-top: 24rpx;
  flex: 1;
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  margin: 28rpx 0 12rpx;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.tag-item {
  min-width: 100rpx;
  height: 88rpx;
  padding: 0 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: var(--ggg-surface-4);
  font-size: 28rpx;
  color: var(--ggg-text-2);
}

.tag-item.active {
  background: #dfece2;
  color: var(--ggg-accent);
  font-weight: 600;
}

.date-picker {
  height: 96rpx;
  background: var(--ggg-surface);
  border: 2rpx solid var(--ggg-border-2);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  font-size: 32rpx;
}

.date-text {
  color: var(--ggg-text-1);
}

.date-hint {
  margin-top: 12rpx;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  gap: 24rpx;
  padding: 24rpx 40rpx calc(40rpx + env(safe-area-inset-bottom));
  background: var(--ggg-surface);
  border-top: 2rpx solid var(--ggg-border-2);
}

.cancel-btn {
  flex: 1;
  background: var(--ggg-surface-4);
}

.save-btn {
  flex: 1;
}

.save-btn.disabled {
  opacity: 0.5;
}

.required-hint {
  margin-top: 20rpx;
  color: var(--ggg-terra);
  font-size: 24rpx;
}
</style>
