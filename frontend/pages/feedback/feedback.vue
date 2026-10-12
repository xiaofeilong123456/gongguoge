<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">意见反馈</view>
      <view class="ggg-back"><GIcon name="feedback" :size="30" color="var(--ggg-terra)" /></view>
    </view>

    <view class="ggg-card form-card">
      <view class="form-label">反馈内容</view>
      <textarea class="ggg-textarea" v-model="content" placeholder="请描述你遇到的问题或建议"></textarea>
      <view class="form-label">联系方式 <text class="ggg-muted">（选填）</text></view>
      <input class="ggg-input" v-model="contact" placeholder="手机号 / 邮箱" />
      <view class="ggg-btn ggg-btn-primary submit-btn" @click="submit"><GIcon name="send" :size="26" color="#ffffff" /> 提交反馈</view>
      <view v-if="notice" class="notice">{{ notice }}</view>
    </view>

    <view class="section-title">历史反馈</view>
    <view class="ggg-card list-card" v-if="list.length">
      <view v-for="item in list" :key="item.id" class="feedback-row">
        <view class="feedback-content">{{ item.content }}</view>
        <view class="feedback-meta">{{ item.contact || '未留联系方式' }} · {{ item.time }}</view>
      </view>
    </view>
    <view class="ggg-card empty-card" v-else>还没有反馈记录</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { feedbackApi } from '../../utils/api.js'

export default {
  components: { GIcon },
  data() {
    return {
      content: '',
      contact: '',
      notice: '',
      list: []
    }
  },
  onShow() {
    this.loadList()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    loadList() {
      feedbackApi.list().then((list) => {
        this.list = (list || []).map(item => ({
          id: item.id,
          content: item.content,
          contact: item.contact,
          time: item.createTime ? item.createTime.substring(0, 16) : ''
        }))
      }).catch(() => {
        this.list = []
      })
    },
    submit() {
      if (!this.content.trim()) {
        this.notice = '请填写反馈内容'
        return
      }
      feedbackApi.submit({
        content: this.content.trim(),
        contact: this.contact.trim()
      }).then(() => {
        this.content = ''
        this.contact = ''
        this.notice = '反馈已提交，感谢你的建议'
        this.loadList()
      }).catch((err) => {
        this.notice = (err && err.message) || '提交失败'
      })
    }
  }
}
</script>

<style scoped>
.form-card {
  margin-top: 28rpx;
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.submit-btn {
  margin-top: 28rpx;
  gap: 8rpx;
}

.notice {
  margin-top: 20rpx;
  color: var(--ggg-accent);
  font-size: 24rpx;
}

.section-title {
  margin: 36rpx 4rpx 16rpx;
  font-weight: 700;
}

.list-card {
  padding: 0;
  overflow: hidden;
}

.feedback-row {
  padding: 24rpx;
  border-bottom: 1rpx solid var(--ggg-border);
}

.feedback-row:last-child {
  border-bottom: none;
}

.feedback-content {
  font-size: 28rpx;
}

.feedback-meta {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: var(--ggg-text-3);
}

.empty-card {
  text-align: center;
  color: var(--ggg-text-3);
}
</style>
