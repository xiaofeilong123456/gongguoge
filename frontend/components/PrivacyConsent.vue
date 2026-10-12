<template>
  <view class="consent-mask">
    <view class="consent-panel">
      <view class="consent-head">
        <view class="consent-title">用户协议与隐私政策</view>
        <view class="consent-version">版本 {{ version }}</view>
      </view>

      <scroll-view class="consent-body" scroll-y>
        <view class="consent-intro">
          欢迎使用功过格。为保障你的知情权，请在开始使用前阅读并同意以下内容。
        </view>

        <view class="consent-block">
          <view class="block-title">用户协议要点</view>
          <view class="block-text">本应用提供行为记录、统计回望与个人数据管理服务。</view>
          <view class="block-text">用户应基于真实、准确的信息使用服务，不得利用本应用从事违法违规行为。</view>
          <view class="block-text">用户对本人创建的分类与记录内容负责，删除的记录会进入回收站。</view>
        </view>

        <view class="consent-block">
          <view class="block-title">隐私政策要点</view>
          <view class="block-text">仅收集手机号、昵称、头像与功过记录等实现功能所必需的信息。</view>
          <view class="block-text">记录数据用于本地展示、统计与跨设备同步，不用于行为画像，不向第三方出售。</view>
          <view class="block-text">你可随时导出或清空个人数据；账号注销后相关数据按规则删除。</view>
          <view class="block-text">V1.0 不接入广告 SDK，不申请与功能无关的系统权限。</view>
        </view>

        <view class="consent-links">
          <text class="consent-link" @click="goPrivacy">查看完整《用户协议》与《隐私政策》</text>
        </view>
      </scroll-view>

      <view class="consent-check" @click="toggle">
        <view class="check-box" :class="{ on: checked }">
          <GIcon v-if="checked" name="check" :size="22" color="#ffffff" />
        </view>
        <view class="check-text">我已阅读并同意《用户协议》与《隐私政策》</view>
      </view>

      <view class="consent-actions">
        <view class="ggg-btn consent-btn ghost" @click="onDisagree">不同意</view>
        <view class="ggg-btn consent-btn primary" :class="{ disabled: !checked }" @click="onAgree">同意并继续</view>
      </view>

      <view class="consent-foot">不同意将无法使用本应用，也不会收集你的任何信息。</view>
    </view>
  </view>
</template>

<script>
import GIcon from './GIcon.vue'
import { PRIVACY_VERSION } from '../utils/privacy.js'

export default {
  name: 'PrivacyConsent',
  components: { GIcon },
  data() {
    return {
      checked: false,
      version: PRIVACY_VERSION
    }
  },
  methods: {
    toggle() {
      this.checked = !this.checked
    },
    onAgree() {
      if (!this.checked) {
        uni.showToast({ title: '请先勾选同意', icon: 'none' })
        return
      }
      this.$emit('agree')
    },
    onDisagree() {
      this.$emit('disagree')
    },
    goPrivacy() {
      uni.navigateTo({ url: '/pages/privacy/privacy' })
    }
  }
}
</script>

<style scoped>
.consent-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  background: rgba(20, 21, 19, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48rpx 40rpx;
  box-sizing: border-box;
}

.consent-panel {
  width: 100%;
  max-width: 640rpx;
  max-height: 82vh;
  box-sizing: border-box;
  background: var(--ggg-surface);
  border: 2rpx solid var(--ggg-border-3);
  border-radius: 36rpx;
  padding: 40rpx 36rpx 32rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.18);
}

.consent-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.consent-title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--ggg-text-1);
}

.consent-version {
  font-size: 22rpx;
  color: var(--ggg-text-faint);
}

.consent-body {
  margin-top: 24rpx;
  max-height: 46vh;
  flex: 1;
  min-height: 0;
}

.consent-intro {
  font-size: 26rpx;
  color: var(--ggg-text-2);
  line-height: 1.7;
}

.consent-block {
  margin-top: 28rpx;
}

.block-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--ggg-text-1);
}

.block-text {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: var(--ggg-text-2);
  line-height: 1.7;
}

.consent-links {
  margin-top: 28rpx;
  padding-bottom: 8rpx;
}

.consent-link {
  font-size: 26rpx;
  color: var(--ggg-accent);
}

.consent-check {
  margin-top: 24rpx;
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.check-box {
  width: 36rpx;
  height: 36rpx;
  flex: none;
  margin-top: 2rpx;
  border-radius: 10rpx;
  border: 2rpx solid var(--ggg-border-strong);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-box.on {
  background: var(--ggg-accent-strong);
  border-color: var(--ggg-accent-strong);
}

.check-text {
  font-size: 26rpx;
  color: var(--ggg-text-1);
  line-height: 1.5;
}

.consent-actions {
  margin-top: 28rpx;
  display: flex;
  gap: 20rpx;
}

.consent-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 28rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.consent-btn.ghost {
  background: var(--ggg-surface-4);
  color: var(--ggg-text-2);
}

.consent-btn.primary {
  background: var(--ggg-accent-strong);
  color: #ffffff;
}

.consent-btn.primary.disabled {
  opacity: 0.45;
}

.consent-foot {
  margin-top: 20rpx;
  font-size: 22rpx;
  color: var(--ggg-text-faint);
  line-height: 1.6;
}
</style>
