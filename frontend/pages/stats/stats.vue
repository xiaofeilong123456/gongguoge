<template>
  <ThemePage>
    <view class="ggg-header">
      <view>
        <view class="ggg-title">统计</view>
        <view class="ggg-muted ggg-small">辅助回望记录节奏，不做分数评价</view>
      </view>
      <GIcon name="chart" :size="40" color="var(--ggg-accent-strong)" />
    </view>

    <view class="mode-switch">
      <view class="mode-item" :class="{ active: mode === 'trend' }" @click="mode = 'trend'">趋势</view>
      <view class="mode-item" :class="{ active: mode === 'overview' }" @click="mode = 'overview'">概览</view>
    </view>

    <template v-if="mode === 'trend'">
      <view class="ggg-card chart-card">
        <view class="chart-title">记录趋势</view>
        <view class="range-row">
          <view v-for="item in [7, 30, 90]" :key="item" class="range-item" :class="{ active: range === item }" @click="changeRange(item)">近{{ item }}天</view>
        </view>
        <view class="trend-chart">
          <canvas
            canvas-id="trendChart"
            id="trendChart"
            class="trend-canvas"
          ></canvas>
        </view>
        <view class="chart-legend">
          <view class="legend-item"><view class="legend-line solid"></view>值得肯定</view>
          <view class="legend-item"><view class="legend-line dashed"></view>有所觉察</view>
        </view>
        <view class="ggg-muted ggg-small legend-tip">两条线各偏移 1.5px 并用实线/虚线区分，数值相同时也能看清</view>
        <view class="insight-box">
          <view class="insight-title">趋势解读</view>
          <view class="insight-text">数据仅用于帮助回望习惯，不构成分数、排名或道德评价。</view>
        </view>
      </view>
    </template>

    <template v-else>
      <view class="ggg-card chart-card">
        <view class="chart-title">本月概览</view>
        <view class="ggg-muted ggg-small">{{ currentMonthText }} · 辅助回望本月记录节奏</view>
        <view class="overview-ring-wrap">
          <view class="overview-ring" :style="ringStyle">
            <view class="ring-center">
              <view class="ring-number">{{ overview.total || 0 }}</view>
              <view class="ggg-small ggg-muted">本月总记录</view>
            </view>
          </view>
          <view class="ggg-small ring-tip">圆环仅表示两类记录构成</view>
        </view>
        <view class="overview-grid">
          <view class="overview-item">
            <view class="overview-icon positive"><GIcon name="check" :size="24" color="var(--ggg-accent)" /></view>
            <view>
              <view class="overview-number positive-text">{{ overview.positive || 0 }}</view>
              <view class="ggg-small ggg-muted">值得肯定</view>
            </view>
          </view>
          <view class="overview-item">
            <view class="overview-icon reflect"><GIcon name="restart" :size="22" color="var(--ggg-terra)" /></view>
            <view>
              <view class="overview-number reflect-text">{{ overview.reflect || 0 }}</view>
              <view class="ggg-small ggg-muted">有所觉察</view>
            </view>
          </view>
          <view class="overview-item days-item">
            <view>
              <view class="ggg-muted ggg-small">有记录天数</view>
              <view class="ggg-small days-sub">截至 {{ currentMonthText }}</view>
            </view>
            <view class="days-number">{{ overview.recordDays || 0 }} <text class="days-unit">天</text></view>
          </view>
        </view>
      </view>

      <view class="ggg-card chart-card">
        <view class="chart-title">本月分类占比</view>
        <view class="ggg-muted ggg-small">{{ currentMonthText }} · 各分类记录数量</view>
        <view class="category-list">
          <view class="category-row" v-for="c in categoryRows" :key="c.categoryId">
            <view class="category-name" :class="c.type === 1 ? 'positive-text' : 'reflect-text'">{{ c.categoryName }}</view>
            <view class="category-bar-wrap">
              <view class="category-bar" :class="c.type === 1 ? 'bar-positive' : 'bar-reflect'" :style="{ width: c.pct + '%' }"></view>
            </view>
            <view class="category-count">{{ c.count }}</view>
          </view>
          <view class="empty-category" v-if="!categoryRows.length">本月还没有记录。</view>
        </view>
      </view>
    </template>

    <PageNav current="stats" />
  </ThemePage>
</template>

<script>
import PageNav from '../../components/PageNav.vue'
import GIcon from '../../components/GIcon.vue'
import { statisticsApi } from '../../utils/api.js'

export default {
  components: { PageNav, GIcon },
  data() {
    return {
      mode: 'trend',
      range: 7,
      overview: {},
      trend: [],
      categoryStats: [],
      drawRetry: 0
    }
  },
  computed: {
    trendData() {
      return this.trend || []
    },
    // 分类占比：按数量降序，附带百分比
    categoryRows() {
      const list = (this.categoryStats || []).filter(c => (c.count || 0) > 0)
      const total = list.reduce((s, c) => s + (c.count || 0), 0) || 1
      return list.slice().sort((a, b) => (b.count || 0) - (a.count || 0)).map(c => ({
        ...c,
        pct: Math.round(((c.count || 0) / total) * 100)
      }))
    },
    currentMonthText() {
      const now = new Date()
      return now.getFullYear() + '年' + (now.getMonth() + 1) + '月'
    },
    ringStyle() {
      const total = this.overview.total || 1
      const positive = this.overview.positive || 0
      const pct = Math.round((positive / total) * 360)
      return {
        background: 'conic-gradient(var(--ggg-accent-strong) 0 ' + pct + 'deg, var(--ggg-terra-strong) ' + pct + 'deg 360deg)'
      }
    }
  },
  onShow() {
    this.loadStats()
  },
  onReady() {
    // canvas 需要等 DOM 就绪后绘制
    setTimeout(() => this.drawTrend(), 50)
  },
  watch: {
    // 「概览 ↔ 趋势」切换时，canvas 会被 v-if 销毁重建，
    // 切回趋势必须重新绘制，否则画布是空白的。
    mode(next) {
      if (next === 'trend') {
        this.$nextTick(() => setTimeout(() => this.drawTrend(), 60))
      }
    }
  },
  methods: {
    loadStats() {
      statisticsApi.get({ range: this.range }).then((res) => {
        this.overview = res.overview || {}
        this.trend = res.trend || []
        this.categoryStats = res.category || []
        this.$nextTick(() => setTimeout(() => this.drawTrend(), 30))
      }).catch(() => {
        this.overview = {}
        this.trend = []
        this.categoryStats = []
      })
    },
    changeRange(range) {
      this.range = range
      this.loadStats()
    },
    // 为纵轴挑选一个「整齐的整数步长」（1/2/5/10/20/50…），
    // 保证刻度值为整数且互不重复。
    niceTickStep(rawMax, targetIntervals) {
      const rough = Math.max(rawMax / Math.max(targetIntervals, 1), 1)
      const mag = Math.pow(10, Math.floor(Math.log(rough) / Math.LN10))
      const norm = rough / mag
      let nice
      if (norm <= 1) nice = 1
      else if (norm <= 2) nice = 2
      else if (norm <= 5) nice = 5
      else nice = 10
      return Math.max(1, Math.round(nice * mag))
    },
    // 用 canvas 绘制趋势折线（微信小程序不支持内联 <svg>）
    drawTrend() {
      // 先查询画布的实际像素尺寸，避免用固定坐标导致错位
      try {
        uni.createSelectorQuery()
          .in(this)
          .select('#trendChart')
          .boundingClientRect((rect) => {
            if (!rect || !rect.width || !rect.height) {
              // 画布尚未布局完成，稍后重试（最多重试若干次）
              if ((this.drawRetry || 0) < 5) {
                this.drawRetry = (this.drawRetry || 0) + 1
                setTimeout(() => this.drawTrend(), 100)
              }
              return
            }
            this.drawRetry = 0
            this.renderTrend(rect.width, rect.height)
          })
          .exec()
      } catch (e) {
        // 查询失败时用兜底尺寸绘制
        this.renderTrend(300, 200)
      }
    },
    // 真正绘制：含坐标轴刻度（纵轴数值、横轴日期）
    renderTrend(W, H) {
      try {
        const ctx = uni.createCanvasContext('trendChart', this)
        const list = this.trendData || []
        const n = list.length || 1

        // 边距：左侧留给纵轴数值，底部留给横轴日期
        const padL = 34
        const padR = 14
        const padT = 14
        const padB = 26
        const innerW = Math.max(W - padL - padR, 10)
        const innerH = Math.max(H - padT - padB, 10)

        // 背景
        ctx.setFillStyle('#f8f5ee')
        ctx.fillRect(0, 0, W, H)

        // 纵轴刻度：先选一个「整齐的整数步长」，保证刻度值不重复。
        // （之前用 max/3 均分再四舍五入，max=2 时会得到 2,1,1,0 这种重复值）
        const rawMax = Math.max(...list.map(item => (item.positive || 0) + (item.reflect || 0)), 1)
        const tickStep = this.niceTickStep(rawMax, 4)
        const axisMax = Math.max(tickStep, Math.ceil(rawMax / tickStep) * tickStep)
        const intervals = Math.max(1, Math.round(axisMax / tickStep))

        ctx.setFontSize(10)
        ctx.setTextAlign('right')
        ctx.setTextBaseline('middle')
        for (let i = 0; i <= intervals; i++) {
          const value = axisMax - tickStep * i
          const y = padT + (innerH * i) / intervals
          // 网格线
          ctx.setStrokeStyle('#dedbd4')
          ctx.setLineWidth(1)
          ctx.beginPath()
          ctx.moveTo(padL, y)
          ctx.lineTo(W - padR, y)
          ctx.stroke()
          // 纵轴数值
          ctx.setFillStyle('#88857f')
          ctx.fillText(String(value), padL - 6, y)
        }

        // 纵轴轴线
        ctx.setStrokeStyle('#d5d0c7')
        ctx.beginPath()
        ctx.moveTo(padL, padT)
        ctx.lineTo(padL, padT + innerH)
        ctx.stroke()

        // 横轴轴线
        ctx.beginPath()
        ctx.moveTo(padL, padT + innerH)
        ctx.lineTo(W - padR, padT + innerH)
        ctx.stroke()

        const step = n > 1 ? innerW / (n - 1) : 0
        const yFor = (v) => padT + innerH - (v / axisMax) * innerH

        // 横轴日期刻度：按点位数量决定显示间隔，避免文字重叠
        const labelEvery = n <= 7 ? 1 : (n <= 30 ? 5 : 15)
        ctx.setFontSize(10)
        ctx.setTextAlign('center')
        ctx.setTextBaseline('top')
        ctx.setFillStyle('#88857f')
        list.forEach((item, i) => {
          if (i % labelEvery !== 0 && i !== n - 1) return
          const x = padL + step * i
          const d = String(item.date || '')
          const short = d.length >= 10 ? d.slice(5) : d
          ctx.fillText(short, x, padT + innerH + 6)
        })

        // 绘制一条折线。offsetY 用于微调纵向位置：
        // 功、过数值相等时两条线会完全重合，各偏移 1.5px 并把「过」画成虚线，
        // 这样即使数值相同也能同时看清两条线。
        const drawLine = (key, color, offsetY, dashed) => {
          ctx.setStrokeStyle(color)
          ctx.setLineWidth(2.5)
          if (typeof ctx.setLineDash === 'function') {
            ctx.setLineDash(dashed ? [6, 4] : [], 0)
          }
          ctx.beginPath()
          list.forEach((item, i) => {
            const x = padL + step * i
            const y = yFor(item[key] || 0) + offsetY
            if (i === 0) ctx.moveTo(x, y)
            else ctx.lineTo(x, y)
          })
          ctx.stroke()
          // 恢复实线，避免影响数据点
          if (typeof ctx.setLineDash === 'function') {
            ctx.setLineDash([], 0)
          }
          // 数据点
          ctx.setFillStyle(color)
          list.forEach((item, i) => {
            const x = padL + step * i
            const y = yFor(item[key] || 0) + offsetY
            ctx.beginPath()
            ctx.arc(x, y, 3, 0, Math.PI * 2)
            ctx.fill()
          })
        }

        drawLine('positive', '#3f8b60', -1.5, false)
        drawLine('reflect', '#b8663e', 1.5, true)

        ctx.draw()
      } catch (e) {
        // 绘制异常时忽略，下次 onShow 会重试
      }
    }
  }
}
</script>

<style scoped>
.mode-switch {
  display: flex;
  margin-top: 32rpx;
  padding: 8rpx;
  background: var(--ggg-surface-3);
  border-radius: 32rpx;
}

.mode-item {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  color: var(--ggg-text-2);
  font-size: 28rpx;
}

.mode-item.active {
  background: var(--ggg-surface);
  color: var(--ggg-accent);
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.chart-card {
  margin-top: 24rpx;
}

.chart-title {
  font-size: 32rpx;
  font-weight: 700;
}

.insight-box {
  margin-top: 28rpx;
  border-top: 1rpx solid #ddd8ce;
  padding-top: 24rpx;
}

.insight-title {
  font-weight: 700;
}

.insight-text {
  margin-top: 10rpx;
  color: var(--ggg-text-2);
  font-size: 24rpx;
  line-height: 1.6;
}

.range-row {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.range-item {
  height: 88rpx;
  padding: 0 32rpx;
  display: flex;
  align-items: center;
  border-radius: 24rpx;
  background: var(--ggg-surface);
  color: var(--ggg-text-2);
  font-size: 28rpx;
}

.range-item.active {
  color: var(--ggg-accent);
  font-weight: 600;
}

.trend-chart {
  height: 520rpx;
  margin-top: 28rpx;
  background: var(--ggg-surface-card-soft);
  border-radius: 32rpx;
  padding: 16rpx;
  box-sizing: border-box;
}

.trend-canvas {
  width: 100%;
  height: 100%;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 48rpx;
  margin-top: 28rpx;
  font-size: 28rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

/* 图例用线条样式，与图中实线/虚线对应 */
.legend-line {
  width: 48rpx;
  height: 6rpx;
  border-radius: 3rpx;
}

.legend-line.solid {
  background: #3f8b60;
}

.legend-line.dashed {
  background: repeating-linear-gradient(
    90deg,
    #b8663e 0,
    #b8663e 12rpx,
    transparent 12rpx,
    transparent 20rpx
  );
}

.legend-tip {
  text-align: center;
  margin-top: 12rpx;
  line-height: 1.5;
}

.overview-ring-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 40rpx;
}

.overview-ring {
  width: 384rpx;
  height: 384rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-center {
  width: 288rpx;
  height: 288rpx;
  border-radius: 50%;
  background: var(--ggg-page-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-number {
  font-size: 80rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ring-tip {
  margin-top: 16rpx;
  color: #8a8780;
}

.overview-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
  margin-top: 40rpx;
}

.overview-item {
  flex: 1 1 calc(50% - 24rpx);
  min-width: 0;
  min-height: 160rpx;
  border-radius: 32rpx;
  background: var(--ggg-surface-card);
  border: 2rpx solid var(--ggg-border-3);
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-sizing: border-box;
}

.overview-item.days-item {
  flex: 1 1 100%;
  justify-content: space-between;
}

.overview-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.overview-icon.positive {
  background: var(--ggg-accent-soft2);
}

.overview-icon.reflect {
  background: var(--ggg-terra-soft);
}

.overview-number {
  font-size: 40rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.positive-text {
  color: var(--ggg-accent);
}

.reflect-text {
  color: var(--ggg-terra);
}

.days-sub {
  color: var(--ggg-text-faint);
  margin-top: 4rpx;
}

.days-number {
  font-size: 48rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.days-unit {
  font-size: 24rpx;
  font-weight: 400;
}

.category-list {
  margin-top: 28rpx;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.category-row:last-child {
  margin-bottom: 0;
}

.category-name {
  width: 120rpx;
  flex-shrink: 0;
  font-size: 26rpx;
  font-weight: 600;
}

.category-bar-wrap {
  flex: 1;
  height: 20rpx;
  border-radius: 10rpx;
  background: var(--ggg-surface-3);
  overflow: hidden;
}

.category-bar {
  height: 100%;
  border-radius: 10rpx;
  min-width: 6rpx;
}

.bar-positive {
  background: var(--ggg-accent-strong);
}

.bar-reflect {
  background: var(--ggg-terra-strong);
}

.category-count {
  width: 56rpx;
  text-align: right;
  flex-shrink: 0;
  font-size: 26rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.empty-category {
  color: var(--ggg-text-3);
  font-size: 26rpx;
  text-align: center;
  padding: 20rpx 0;
}
</style>
