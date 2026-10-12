<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">修改密码</view>
      <view class="ggg-back"></view>
    </view>

    <view class="ggg-card form-card">
      <view class="form-item">
        <view class="form-label">原密码</view>
        <input class="ggg-input" v-model="oldPassword" password placeholder="从未设置过密码可留空" />
      </view>
      <view class="form-item">
        <view class="form-label">新密码</view>
        <input class="ggg-input" v-model="newPassword" password placeholder="6-32 位" />
      </view>
      <view class="form-item">
        <view class="form-label">确认新密码</view>
        <input class="ggg-input" v-model="confirmPassword" password placeholder="再次输入新密码" />
      </view>
      <view class="ggg-muted ggg-small form-hint">若你一直用验证码登录、从未设置过密码，原密码可留空直接设置。</view>
    </view>

    <view class="ggg-btn ggg-btn-primary submit-btn" @click="submit">保存新密码</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { authApi } from '../../utils/api.js'

export default {
  components: { GIcon },
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
      saving: false
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    submit() {
      const np = this.newPassword
      if (!np) {
        uni.showToast({ title: '请输入新密码', icon: 'none' })
        return
      }
      if (np.length < 6 || np.length > 32) {
        uni.showToast({ title: '新密码需 6-32 位', icon: 'none' })
        return
      }
      if (np !== this.confirmPassword) {
        uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
        return
      }
      if (this.saving) return
      this.saving = true
      authApi.changePassword({ oldPassword: this.oldPassword, newPassword: np }).then(() => {
        uni.showToast({ title: '密码已更新', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      }).catch((err) => {
        uni.showToast({ title: (err && err.message) || '修改失败', icon: 'none' })
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.form-card {
  margin-top: 28rpx;
}

.form-item {
  margin-bottom: 28rpx;
}

.form-item:last-of-type {
  margin-bottom: 0;
}

.form-label {
  margin-bottom: 12rpx;
  font-weight: 600;
  color: var(--ggg-text-1);
}

.form-hint {
  margin-top: 20rpx;
  line-height: 1.6;
}

.submit-btn {
  margin-top: 40rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 600;
}
</style>
