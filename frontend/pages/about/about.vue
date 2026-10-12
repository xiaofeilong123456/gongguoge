<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">关于我们</view>
      <view class="ggg-back"><GIcon name="info" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="brand-wrap">
      <view class="brand-ring">功过<br />格</view>
      <view class="brand-name">功过格</view>
      <view class="brand-slogan">日日自省，徐徐而进</view>
      <view class="brand-version">V1.0.0</view>
    </view>

    <view class="ggg-card info-card">
      <view class="info-row"><view class="info-label"><GIcon name="user" :size="26" color="var(--ggg-accent)" /> 开发者</view><view class="info-value">个人开发者</view></view>
      <view class="info-row"><view class="info-label"><GIcon name="shield" :size="26" color="var(--ggg-accent)" /> 数据原则</view><view class="info-value">本地优先，功过分开记录</view></view>
      <view class="info-row"><view class="info-label"><GIcon name="bell" :size="26" color="var(--ggg-accent)" /> 版本说明</view><view class="info-value">V1.0 不含广告与社交排名</view></view>
    </view>

    <view class="action-row">
      <view class="ggg-btn ggg-btn-ghost action-btn" @click="go('/pages/privacy/privacy')"><GIcon name="shield" :size="26" color="var(--ggg-brown)" /> 协议与隐私</view>
      <view class="ggg-btn ggg-btn-primary action-btn" @click="go('/pages/feedback/feedback')"><GIcon name="feedback" :size="26" color="#ffffff" /> 意见反馈</view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'

export default {
  components: { GIcon },
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
.brand-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 56rpx;
}

.brand-ring {
  width: 192rpx;
  height: 192rpx;
  border-radius: 50%;
  border: 18rpx solid rgba(40, 43, 38, 0.16);
  box-shadow: inset 4rpx 0 0 rgba(40, 43, 38, 0.13), 0 0 0 6rpx rgba(40, 43, 38, 0.035);
  transform: rotate(-4deg);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.25;
}

.brand-name {
  margin-top: 28rpx;
  font-size: 36rpx;
  font-weight: 700;
}

.brand-slogan {
  margin-top: 12rpx;
  color: var(--ggg-text-2);
  font-size: 26rpx;
}

.brand-version {
  margin-top: 8rpx;
  color: var(--ggg-text-faint);
  font-size: 22rpx;
}

.info-card {
  margin-top: 40rpx;
  padding: 0;
  overflow: hidden;
}

.info-row {
  display: flex;
  min-height: 104rpx;
  align-items: center;
  padding: 0 24rpx;
  border-bottom: 1rpx solid var(--ggg-border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 180rpx;
  color: var(--ggg-text-3);
  font-size: 26rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.info-value {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
}

.action-row {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.action-btn {
  flex: 1;
  gap: 8rpx;
}
</style>
