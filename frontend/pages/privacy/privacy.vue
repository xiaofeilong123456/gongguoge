<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">协议与隐私</view>
      <view class="ggg-back"><GIcon name="shield" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="privacy-switch">
      <view class="privacy-mode" :class="{ active: tab === 'agreement' }" @click="tab = 'agreement'">用户协议</view>
      <view class="privacy-mode" :class="{ active: tab === 'policy' }" @click="tab = 'policy'">隐私政策</view>
    </view>

    <view class="ggg-card content-card">
      <template v-if="tab === 'agreement'">
        <view class="content-title">功过格用户协议</view>
        <view class="content-text">本应用提供行为记录、统计回望与个人数据管理服务。</view>
        <view class="content-text">用户应基于真实、准确的信息使用服务，不得利用本应用从事违法违规行为。</view>
        <view class="content-text">用户对本人创建的分类与记录内容负责，删除记录将进入 7 天回收站。</view>
        <view class="content-text">本协议更新后将在应用中公示，重大变更会以明显方式提醒。</view>
      </template>
      <template v-else>
        <view class="content-title">功过格隐私政策</view>
        <view class="content-text">本应用仅收集手机号、昵称、头像与功过记录等必要信息。</view>
        <view class="content-text">记录数据用于本地展示、统计和跨设备同步，不用于行为画像。</view>
        <view class="content-text">用户可随时导出或清空个人数据，账号注销后相关数据会按规则删除。</view>
        <view class="content-text">V1.0 不接入广告 SDK，不申请与功能无关的系统权限。</view>
      </template>
    </view>

    <view class="ggg-btn ggg-btn-ghost about-btn" @click="go('/pages/about/about')"><GIcon name="info" :size="26" color="var(--ggg-brown)" /> 关于我们</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'

export default {
  components: { GIcon },
  data() {
    return {
      tab: 'agreement'
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
.privacy-switch {
  display: flex;
  padding: 8rpx;
  background: var(--ggg-surface-3);
  border-radius: 32rpx;
  margin-top: 32rpx;
}

.privacy-mode {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  color: var(--ggg-text-2);
  font-size: 28rpx;
}

.privacy-mode.active {
  background: var(--ggg-surface);
  color: var(--ggg-accent);
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.content-card {
  margin-top: 24rpx;
}

.content-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}

.content-text {
  margin-top: 16rpx;
  line-height: 1.7;
  color: var(--ggg-text-1);
  font-size: 28rpx;
}

.about-btn {
  margin-top: 24rpx;
  gap: 8rpx;
}
</style>
