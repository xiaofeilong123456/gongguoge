<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">提醒设置</view>
      <view class="ggg-back"><GIcon name="bell" :size="30" color="var(--ggg-terra)" /></view>
    </view>

    <view class="ggg-card">
      <view class="reminder-row">
        <view>
          <view class="reminder-title">每日复盘提醒</view>
          <view class="ggg-muted ggg-small">每天固定时间提醒记录</view>
        </view>
        <view class="switch" :class="{ on: enabled }" @click="toggleEnabled">
          <view class="switch-dot" :class="{ on: enabled }"></view>
        </view>
      </view>

      <view class="time-label">提醒时间</view>
      <picker mode="time" :value="time" @change="onTimeChange" :disabled="!enabled">
        <view class="time-picker" :class="{ disabled: !enabled }">{{ time }}</view>
      </picker>

      <!-- 平台能力提示：不做假的「已开启」 -->
      <view v-if="enabled && !supported" class="platform-tip">
        <GIcon name="bell" :size="26" color="var(--ggg-terra-strong)" />
        <text>当前运行环境（浏览器 / 小程序）不支持本地通知。打包为安卓 App 后提醒才会真正生效。</text>
      </view>

      <view class="ggg-btn ggg-btn-primary save-btn" @click="save">
        <GIcon name="save" :size="26" color="#ffffff" /> 保存提醒
      </view>
      <view v-if="notice" class="notice">{{ notice }}</view>

      <view class="ggg-muted ggg-small footnote">
        说明：受安卓系统限制，本地通知的定时触发依赖 App 进程存活。若你把 App 从后台清除、或系统回收了进程，当天提醒可能不会准时弹出，会在你下次打开 App 时补发。要做到「完全退出也能准时提醒」，需要接入服务端离线推送（如 uni-push）。
      </view>
    </view>

    <view class="ggg-card preview-card">
      <view class="preview-title">通知预览</view>
      <view class="preview-bubble">
        <view class="preview-app">功过格</view>
        <view class="preview-text">该做今天的复盘了，花一分钟记下一次觉察。</view>
      </view>
      <view class="ggg-btn ggg-btn-ghost test-btn" @click="sendTest">
        <GIcon name="bell" :size="26" color="var(--ggg-brown)" /> 发送一条测试通知
      </view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import {
  loadReminder,
  saveReminder,
  canNotify,
  notifyNow,
  applyReminder,
  requestNotificationPermission
} from '../../utils/reminder.js'
import { explainPermission, hasShownPermission } from '../../utils/permission.js'

export default {
  components: { GIcon },
  data() {
    return {
      enabled: false,
      time: '21:00',
      notice: '',
      supported: false
    }
  },
  onLoad() {
    // 修掉「假开关」：进入页面必须回显已保存的设置
    const saved = loadReminder()
    this.enabled = saved.enabled
    this.time = saved.time
    this.supported = canNotify()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    /**
     * 拨动开关即生效：开启时申请权限并立即预约，关闭时立即取消。
     * 之前开关只改 UI 状态、必须再点「保存提醒」才真正生效，容易让人以为开关坏了。
     */
    async toggleEnabled() {
      this.notice = ''

      // 关闭：直接生效，不需要任何权限
      if (this.enabled) {
        this.enabled = false
        saveReminder({ enabled: false, time: this.time })
        this.notice = '已关闭每日复盘提醒'
        return
      }

      // 打开：先按合规要求说明用途，再申请权限
      const allowed = await this.ensureNotificationPermission()
      this.enabled = allowed
      if (!allowed) {
        this.notice = '未开启通知权限，提醒不会生效'
        saveReminder({ enabled: false, time: this.time })
        return
      }
      // 权限拿到后立刻保存并预约，不需要再点保存
      this.applyAndReport()
    },
    // 保存并预约，同时把结果写到 notice
    applyAndReport() {
      if (!this.supported) {
        saveReminder({ enabled: true, time: this.time })
        this.notice = '设置已保存。当前环境无通知能力，打包为安卓 App 后生效。'
        return
      }
      const result = applyReminder({ enabled: true, time: this.time })
      if (result.action === 'scheduled' || result.action === 'already-scheduled') {
        this.notice = '已预约，每天 ' + this.time + ' 提醒你复盘'
      } else if (result.action === 'fired') {
        this.notice = '提醒已保存，并补发了今天的通知'
      } else if (result.action === 'already-fired') {
        this.notice = '提醒已保存，今天已提醒过'
      } else {
        this.notice = '提醒已保存'
      }
    },
    // 合规要求：申请权限前必须先说明用途（仅首次说明，避免反复打扰）
    async ensureNotificationPermission() {
      if (!this.supported) {
        // 无通知能力的平台（H5 / 小程序）不做权限申请，由页面提示说明
        return true
      }
      if (!hasShownPermission('notification')) {
        const proceed = await explainPermission('notification')
        if (!proceed) return false
      }
      const status = await requestNotificationPermission()
      return status === 'granted'
    },
    onTimeChange(e) {
      this.time = e.detail.value
      this.notice = ''
    },
    async save() {
      if (!this.enabled) {
        saveReminder({ enabled: false, time: this.time })
        this.notice = '已关闭每日复盘提醒'
        return
      }
      // 首次开启时按合规要求先说明用途再申请权限
      if (this.supported && !hasShownPermission('notification')) {
        const allowed = await this.ensureNotificationPermission()
        if (!allowed) {
          this.enabled = false
          saveReminder({ enabled: false, time: this.time })
          this.notice = '未开启通知权限，提醒不会生效'
          return
        }
      }
      this.applyAndReport()
    },
    sendTest() {
      if (!this.supported) {
        uni.showToast({ title: '当前环境不支持本地通知', icon: 'none' })
        return
      }
      const ok = notifyNow('该做今天的复盘了，花一分钟记下一次觉察。')
      if (ok) {
        uni.showToast({ title: '测试通知已发送', icon: 'none' })
      } else {
        uni.showToast({ title: '发送失败，请检查通知权限', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
.ggg-card {
  margin-top: 28rpx;
}

.reminder-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reminder-title {
  font-size: 30rpx;
  font-weight: 700;
}

.switch {
  width: 96rpx;
  height: 56rpx;
  border-radius: 28rpx;
  background: var(--ggg-border-strong);
  position: relative;
}

.switch.on {
  background: var(--ggg-green-fill);
}

.switch-dot {
  position: absolute;
  top: 8rpx;
  left: 8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #ffffff;
  transition: left 0.2s;
}

.switch-dot.on {
  left: 48rpx;
}

.time-label {
  margin-top: 40rpx;
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.time-picker {
  margin-top: 12rpx;
  height: 88rpx;
  background: var(--ggg-surface);
  border: 1rpx solid var(--ggg-border-2);
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  font-size: 30rpx;
}

.time-picker.disabled {
  opacity: 0.55;
}

.platform-tip {
  margin-top: 24rpx;
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: var(--ggg-terra-soft);
  color: var(--ggg-error-text);
  font-size: 24rpx;
  line-height: 1.6;
}

.save-btn {
  margin-top: 32rpx;
  gap: 8rpx;
}

.notice {
  margin-top: 20rpx;
  color: var(--ggg-accent);
  font-size: 24rpx;
}

.footnote {
  margin-top: 24rpx;
  line-height: 1.7;
}

.preview-card {
  margin-top: 24rpx;
}

.preview-title {
  font-size: 30rpx;
  font-weight: 700;
}

.preview-bubble {
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: var(--ggg-surface-3);
  border: 1rpx solid var(--ggg-border-2);
}

.preview-app {
  font-size: 22rpx;
  color: var(--ggg-text-3);
}

.preview-text {
  margin-top: 10rpx;
  font-size: 28rpx;
  color: var(--ggg-text-1);
  line-height: 1.6;
}

.test-btn {
  margin-top: 24rpx;
  gap: 8rpx;
}
</style>
