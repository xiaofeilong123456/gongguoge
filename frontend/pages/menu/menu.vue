<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">全部页面</view>
      <view class="ggg-back"></view>
    </view>

    <view class="ggg-muted ggg-small menu-desc">补充功能入口，现有页面保持不变</view>
    <view class="menu-grid">
      <view v-for="item in items" :key="item.url" class="menu-card" @click="go(item.url)">
        <view class="menu-icon" :class="item.tone"><GIcon :name="item.icon" :size="28" :color="item.color" /></view>
        <view class="menu-name">{{ item.name }}</view>
        <view class="menu-desc">{{ item.desc }}</view>
      </view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'

export default {
  components: { GIcon },
  data() {
    return {
      items: [
        { url: '/pages/auth/auth', name: '登录 / 注册', desc: '手机号验证码登录', icon: 'user', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/profile/profile', name: '编辑资料', desc: '昵称与头像', icon: 'edit', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/categories/categories', name: '分类管理', desc: '功类与过类', icon: 'tag', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/recycle/recycle', name: '回收站', desc: '7 天可恢复', icon: 'recycle', color: 'var(--ggg-terra)', tone: 'ochre' },
        { url: '/pages/feedback/feedback', name: '意见反馈', desc: '问题与建议', icon: 'feedback', color: 'var(--ggg-terra)', tone: 'ochre' },
        { url: '/pages/export/export', name: '数据导出', desc: 'JSON / CSV', icon: 'export', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/reminder/reminder', name: '提醒设置', desc: '每日复盘提醒', icon: 'bell', color: 'var(--ggg-terra)', tone: 'ochre' },
        { url: '/pages/appearance/appearance', name: '显示设置', desc: '主题与字号', icon: 'palette', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/privacy/privacy', name: '协议与隐私', desc: '用户协议 / 隐私政策', icon: 'shield', color: 'var(--ggg-terra)', tone: 'ochre' },
        { url: '/pages/permissions/permissions', name: '权限管理', desc: '权限用途说明', icon: 'lock', color: 'var(--ggg-accent)', tone: 'green' },
        { url: '/pages/about/about', name: '关于我们', desc: '版本与开发者', icon: 'info', color: 'var(--ggg-accent)', tone: 'green' }
      ]
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style scoped>
.menu-desc {
  margin-top: 16rpx;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-top: 28rpx;
}

.menu-card {
  min-height: 208rpx;
  box-sizing: border-box;
  background: var(--ggg-surface-card);
  border: 2rpx solid #e6e1d8;
  border-radius: 32rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.menu-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.menu-icon.green {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
}

.menu-icon.ochre {
  background: var(--ggg-terra-soft);
  color: var(--ggg-terra);
}

.menu-name {
  margin-top: 18rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.menu-desc {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--ggg-text-3);
}
</style>
