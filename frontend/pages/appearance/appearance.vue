<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">显示设置</view>
      <view class="ggg-back"><GIcon name="palette" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="ggg-card">
      <view class="setting-title">主题模式</view>
      <view class="option-grid">
        <view
          v-for="item in themeOptions"
          :key="item.value"
          class="option-item"
          :class="{ active: theme === item.value }"
          @click="theme = item.value"
        >{{ item.label }}</view>
      </view>
    </view>

    <view class="ggg-card">
      <view class="setting-title">字体大小</view>
      <view class="option-grid">
        <view
          v-for="item in fontOptions"
          :key="item.value"
          class="option-item"
          :class="{ active: fontSize === item.value }"
          @click="fontSize = item.value"
        >{{ item.label }}</view>
      </view>
    </view>

    <view class="ggg-btn ggg-btn-primary save-btn" @click="save"><GIcon name="save" :size="26" color="#ffffff" /> 保存显示设置</view>
    <view v-if="notice" class="notice">{{ notice }}</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { loadSavedAppearance, saveAppearance } from '../../utils/appearance.js'

export default {
  components: { GIcon },
  data() {
    const saved = loadSavedAppearance()
    return {
      theme: saved.theme || 'system',
      fontSize: saved.fontSize || 'medium',
      notice: '',
      themeOptions: [
        { value: 'light', label: '浅色' },
        { value: 'dark', label: '深色' },
        { value: 'system', label: '跟随系统' }
      ],
      fontOptions: [
        { value: 'small', label: '小' },
        { value: 'medium', label: '中' },
        { value: 'large', label: '大' }
      ]
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    save() {
      saveAppearance({ theme: this.theme, fontSize: this.fontSize })
      this.notice = '显示设置已保存'
    }
  }
}
</script>

<style scoped>
.ggg-card {
  margin-top: 24rpx;
}

.setting-title {
  font-size: 30rpx;
  font-weight: 700;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 20rpx;
}

.option-item {
  height: 88rpx;
  border-radius: 24rpx;
  background: var(--ggg-surface-4);
  color: var(--ggg-text-2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.option-item.active {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
  font-weight: 600;
}

.save-btn {
  margin-top: 28rpx;
  gap: 8rpx;
}

.notice {
  margin-top: 20rpx;
  color: var(--ggg-accent);
  font-size: 24rpx;
}
</style>
