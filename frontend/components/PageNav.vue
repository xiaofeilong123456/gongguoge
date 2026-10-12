<template>
  <view class="ggg-tabbar">
    <view class="ggg-tabbar-item" :class="{ active: current === 'index' }" @click="go('/pages/index/index')">
      <GIcon name="home" :size="44" />
      <text>今日</text>
    </view>
    <view class="ggg-tabbar-item" :class="{ active: current === 'stats' }" @click="go('/pages/stats/stats')">
      <GIcon name="chart" :size="44" />
      <text>统计</text>
    </view>
    <view class="ggg-tabbar-item" :class="{ active: current === 'settings' }" @click="go('/pages/settings/settings')">
      <GIcon name="settings" :size="44" />
      <text>设置</text>
    </view>
  </view>
</template>

<script>
import GIcon from './GIcon.vue'

export default {
  name: 'PageNav',
  components: { GIcon },
  props: {
    current: {
      type: String,
      default: 'index'
    }
  },
  methods: {
    go(url) {
      uni.reLaunch({ url })
    }
  }
}
</script>

<style scoped>
.ggg-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  background: var(--ggg-surface-card);
  border-top: 2rpx solid var(--ggg-border);
  padding: 8rpx 40rpx 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.ggg-tabbar-item {
  flex: 1;
  min-height: 108rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: var(--ggg-text-2);
  gap: 4rpx;
}

.ggg-tabbar-item.active {
  color: var(--ggg-accent-strong);
  font-weight: 600;
}
</style>
