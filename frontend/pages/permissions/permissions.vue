<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">权限管理</view>
      <view class="ggg-back"><GIcon name="shield" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="intro-card ggg-card">
      <view class="intro-title">我们只申请功能必需的权限</view>
      <view class="ggg-muted ggg-small intro-text">
        每个权限在申请前都会先说明用途。拒绝任何一项都不会影响记录与统计等核心功能。
      </view>
    </view>

    <view class="ggg-card group-card" v-for="item in list" :key="item.key">
      <view class="perm-row">
        <view class="perm-icon">
          <GIcon :name="item.key === 'notification' ? 'bell' : 'export'" :size="28" color="var(--ggg-accent)" />
        </view>
        <view class="perm-main">
          <view class="perm-title">{{ item.title }}</view>
          <view class="ggg-muted ggg-small perm-purpose">{{ item.purpose }}</view>
        </view>
      </view>
      <view class="perm-state">
        <text class="state-label">说明状态</text>
        <text class="state-value" :class="{ done: !!item.explainedAt }">
          {{ item.explainedAt ? '已说明' : '未说明' }}
        </text>
      </view>
    </view>

    <view class="ggg-btn reset-btn" @click="resetExplained">
      <GIcon name="restart" :size="26" color="var(--ggg-brown)" /> 重置说明状态
    </view>
    <view class="ggg-muted ggg-small foot-note">
      重置后，下次触发对应功能时会重新向你说明用途。系统权限本身仍需在手机「设置 → 应用 → 功过格 → 权限」中管理。
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { permissionOverview, resetPermissionShown } from '../../utils/permission.js'

export default {
  components: { GIcon },
  data() {
    return {
      list: []
    }
  },
  onShow() {
    this.list = permissionOverview()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    resetExplained() {
      uni.showModal({
        title: '重置说明状态',
        content: '重置后，相关功能会再次向你说明权限用途。是否继续？',
        success: (res) => {
          if (!res.confirm) return
          resetPermissionShown()
          this.list = permissionOverview()
          uni.showToast({ title: '已重置', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style scoped>
.intro-card {
  margin-top: 28rpx;
}

.intro-title {
  font-size: 32rpx;
  font-weight: 700;
}

.intro-text {
  margin-top: 12rpx;
  line-height: 1.7;
}

.group-card {
  margin-top: 24rpx;
}

.perm-row {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.perm-icon {
  width: 64rpx;
  height: 64rpx;
  flex: none;
  border-radius: 50%;
  background: var(--ggg-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}

.perm-main {
  flex: 1;
  min-width: 0;
}

.perm-title {
  font-size: 30rpx;
  font-weight: 700;
}

.perm-purpose {
  margin-top: 8rpx;
  line-height: 1.6;
}

.perm-state {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid var(--ggg-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.state-label {
  font-size: 24rpx;
  color: var(--ggg-text-2);
}

.state-value {
  font-size: 24rpx;
  color: var(--ggg-text-faint);
}

.state-value.done {
  color: var(--ggg-accent);
  font-weight: 600;
}

.reset-btn {
  margin-top: 28rpx;
  border: 2rpx solid var(--ggg-border-ghost);
  color: var(--ggg-brown);
  background: transparent;
  gap: 8rpx;
}

.foot-note {
  margin-top: 20rpx;
  line-height: 1.7;
}
</style>
