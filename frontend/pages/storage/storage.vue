<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">本地存储与隐私</view>
      <view class="ggg-back"><GIcon name="shield" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="ggg-card">
      <view class="block-title"><GIcon name="lock" :size="30" color="var(--ggg-accent)" /> 数据默认保存在本机</view>
      <view class="ggg-muted ggg-small block-desc">功过记录、分类、回收站和反馈数据默认只存储在本地。</view>
    </view>

    <view class="ggg-card">
      <view class="block-title">本机包含的数据</view>
      <view class="storage-row"><view class="storage-label">记录</view><view class="storage-value">{{ entryCount }} 条</view></view>
      <view class="storage-row"><view class="storage-label">分类</view><view class="storage-value">{{ categoryCount }} 个</view></view>
      <view class="storage-row"><view class="storage-label">回收站</view><view class="storage-value">{{ recycleCount }} 条</view></view>
    </view>

    <view class="ggg-card">
      <view class="block-title">隐私说明</view>
      <view class="privacy-line">本应用仅收集手机号、昵称、头像与功过记录等必要信息。</view>
      <view class="privacy-line">记录数据用于本地展示、统计和跨设备同步，不用于行为画像。</view>
      <view class="privacy-line">用户可以随时导出或清空个人数据。</view>
    </view>

    <view class="action-row">
      <view class="ggg-btn ggg-btn-ghost action-btn" @click="go('/pages/export/export')"><GIcon name="export" :size="26" color="var(--ggg-brown)" /> 数据导出</view>
      <view class="ggg-btn ggg-btn-primary action-btn" @click="go('/pages/privacy/privacy')"><GIcon name="shield" :size="26" color="#ffffff" /> 协议与隐私</view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { recordApi, categoryApi } from '../../utils/api.js'

export default {
  components: { GIcon },
  data() {
    return {
      entryCount: 0,
      categoryCount: 0,
      recycleCount: 0
    }
  },
  onShow() {
    this.loadCounts()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    go(url) {
      uni.navigateTo({ url })
    },
    loadCounts() {
      recordApi.list().then((list) => {
        this.entryCount = (list || []).length
      }).catch(() => {
        this.entryCount = 0
      })
      categoryApi.list().then((list) => {
        this.categoryCount = (list || []).length
      }).catch(() => {
        this.categoryCount = 0
      })
      recordApi.recycle().then((list) => {
        this.recycleCount = (list || []).length
      }).catch(() => {
        this.recycleCount = 0
      })
    }
  }
}
</script>

<style scoped>
.ggg-card {
  margin-top: 24rpx;
}

.block-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.block-desc {
  margin-top: 12rpx;
  line-height: 1.6;
}

.storage-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 84rpx;
  border-bottom: 1rpx solid var(--ggg-border);
  font-size: 28rpx;
}

.storage-row:last-child {
  border-bottom: none;
}

.storage-label {
  color: var(--ggg-text-2);
}

.storage-value {
  font-weight: 600;
}

.privacy-line {
  margin-top: 16rpx;
  color: #4b4c48;
  font-size: 26rpx;
  line-height: 1.6;
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
