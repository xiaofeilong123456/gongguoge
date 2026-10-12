<template>
  <ThemePage @touchstart.native="onTouchStart" @touchend.native="onTouchEnd">
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">日历回望</view>
      <view class="ggg-back"></view>
    </view>

    <view class="month-head">
      <view class="month-nav" @click="prevMonth"><GIcon name="back" :size="30" color="var(--ggg-terra)" /></view>
      <view class="month-title">{{ year }}年{{ month }}月</view>
      <view class="month-nav" @click="nextMonth"><GIcon name="arrow" :size="30" color="var(--ggg-terra)" /></view>
      <view class="month-hint">有记录日期会高亮标记</view>
    </view>
    <view class="week-row">
      <view v-for="day in ['日','一','二','三','四','五','六']" :key="day" class="week-day">{{ day }}</view>
    </view>
    <view class="day-grid">
      <view v-for="i in leadingBlanks" :key="'empty-' + i" class="day-cell"></view>
      <view
        v-for="cell in dayCells"
        :key="cell.date"
        class="day-cell"
        :class="{ selected: selectedDay === cell.day, hasRecord: hasRecord(cell.date) }"
        @click="selectDay(cell.day)"
      >
        <view class="day-number">{{ cell.day }}</view>
        <view class="record-dot" v-if="hasRecord(cell.date)"></view>
      </view>
    </view>

    <view class="ggg-card detail-card">
      <view class="detail-title">{{ month }}月{{ selectedDay }}日</view>
      <view class="detail-summary">
        <view class="detail-summary-item">
          <view class="type-icon positive">
            <view class="type-icon-inner"><GIcon name="check" :size="28" color="#ffffff" /></view>
          </view>
          <text>值得肯定</text>
          <text class="detail-count positive-text">{{ dayPositiveCount }}</text>
        </view>
        <view class="detail-summary-item">
          <view class="type-icon reflect">
            <view class="type-icon-inner"><GIcon name="restart" :size="22" color="var(--ggg-terra-strong)" /></view>
          </view>
          <text>有所觉察</text>
          <text class="detail-count reflect-text">{{ dayReflectCount }}</text>
        </view>
      </view>
      <view class="detail-list" v-if="dayEntries.length">
        <view v-for="item in dayEntries" :key="item.id" class="detail-row">
          <view class="detail-dot" :class="item.type"></view>
          <view class="detail-text">{{ item.text }}</view>
          <view class="ggg-small ggg-muted">{{ item.time }}</view>
        </view>
      </view>
      <view class="empty-detail" v-else>这一天还没有记录。</view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { recordApi } from '../../utils/api.js'
import { formatDate } from '../../utils/store.js'

export default {
  components: { GIcon },
  data() {
    return {
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
      selectedDay: new Date().getDate(),
      entries: [],
      today: formatDate(new Date()),
      touchStartX: 0
    }
  },
  computed: {
    leadingBlanks() {
      return new Date(this.year, this.month - 1, 1).getDay()
    },
    daysInMonth() {
      return new Date(this.year, this.month, 0).getDate()
    },
    dayCells() {
      return Array.from({ length: this.daysInMonth }, (_, index) => {
        const day = index + 1
        return {
          day,
          date: this.year + '-' + String(this.month).padStart(2, '0') + '-' + String(day).padStart(2, '0')
        }
      })
    },
    dayEntries() {
      const day = String(this.selectedDay).padStart(2, '0')
      const date = this.year + '-' + String(this.month).padStart(2, '0') + '-' + day
      return this.entries.filter(item => item.date === date)
    },
    dayPositiveCount() {
      return this.dayEntries.filter(item => item.type === 'positive').length
    },
    dayReflectCount() {
      return this.dayEntries.filter(item => item.type === 'reflect').length
    }
  },
  onShow() {
    this.loadRecords()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    loadRecords() {
      recordApi.list().then((list) => {
        this.entries = (list || []).map(item => ({
          id: item.id,
          type: item.type === 1 ? 'positive' : 'reflect',
          text: item.content,
          tag: item.categoryName || '',
          time: item.createTime ? item.createTime.substring(11, 16) : '',
          date: item.recordDate
        }))
      }).catch(() => {
        this.entries = []
      })
    },
    selectDay(day) {
      this.selectedDay = day
    },
    prevMonth() {
      if (this.month === 1) {
        this.year -= 1
        this.month = 12
      } else {
        this.month -= 1
      }
      this.selectedDay = 1
    },
    nextMonth() {
      if (this.month === 12) {
        this.year += 1
        this.month = 1
      } else {
        this.month += 1
      }
      this.selectedDay = 1
    },
    onTouchStart(e) {
      this.touchStartX = e.touches[0].clientX
    },
    onTouchEnd(e) {
      const deltaX = e.changedTouches[0].clientX - this.touchStartX
      if (Math.abs(deltaX) > 40) {
        if (deltaX < 0) {
          this.nextMonth()
        } else {
          this.prevMonth()
        }
      }
    },
    hasRecord(date) {
      return this.entries.some(item => item.date === date)
    }
  }
}
</script>

<style scoped>
.ggg-title {
  font-size: 40rpx;
}

.month-title {
  font-size: 36rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.month-head {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16rpx;
  position: relative;
}

.month-hint {
  width: 100%;
  text-align: center;
  color: var(--ggg-text-2);
  font-size: 22rpx;
}

.month-nav {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ggg-terra);
}

.week-row,
.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.week-row {
  margin-top: 36rpx;
}

.week-day,
.day-number {
  text-align: center;
}

.week-day {
  color: var(--ggg-text-2);
  font-size: 24rpx;
}

.day-grid {
  margin-top: 24rpx;
  row-gap: 16rpx;
}

.day-cell {
  height: 96rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
}

.day-cell.selected {
  background: var(--ggg-surface);
  border: 4rpx solid #8fb69d;
}

.record-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--ggg-terra-strong);
  margin-top: 6rpx;
}

.detail-card {
  margin-top: 40rpx;
}

.detail-title {
  font-size: 32rpx;
  font-weight: 700;
}

.detail-summary {
  display: flex;
  gap: 48rpx;
  margin-top: 20rpx;
  font-size: 28rpx;
}

.detail-summary-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.type-icon {
  width: 56rpx;
  height: 56rpx;
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

.detail-count {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.positive-text {
  color: var(--ggg-accent);
}

.reflect-text {
  color: var(--ggg-terra);
}

.detail-list {
  margin-top: 24rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 88rpx;
  border-bottom: 2rpx solid var(--ggg-border);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-dot.positive {
  background: var(--ggg-green-fill);
}

.detail-dot.reflect {
  border: 6rpx solid var(--ggg-terra-strong);
}

.detail-text {
  flex: 1;
  font-size: 28rpx;
}

.empty-detail {
  margin-top: 20rpx;
  color: var(--ggg-text-3);
  font-size: 28rpx;
}
</style>
