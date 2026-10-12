<template>
  <ThemePage>
    <view class="ggg-header">
      <view v-if="canBack" class="ggg-back" @click="goBack"><GIcon name="back" :size="44" color="var(--ggg-terra)" /></view>
      <view v-else class="ggg-back"></view>
      <view class="ggg-title">登录 / 注册</view>
      <view class="ggg-back"><GIcon name="user" :size="40" color="var(--ggg-accent-strong)" /></view>
    </view>

    <view class="ggg-card auth-card">
      <view class="auth-tip">未注册的手机号验证通过后将自动注册</view>
      <view class="field-item">
        <view class="field-label">手机号</view>
        <input class="field-input" type="number" maxlength="11" v-model="phone" placeholder="请输入手机号" />
      </view>
      <view class="field-item">
        <view class="field-label">验证码</view>
        <view class="code-row">
          <input class="field-input code-input" type="number" maxlength="8" v-model="code" placeholder="请输入收到的验证码" />
          <view class="code-btn" @click="sendCode">获取验证码</view>
        </view>
      </view>

      <view v-if="notice" class="notice">{{ notice }}</view>
      <view class="ggg-btn ggg-btn-primary submit-btn" @click="submit">登录 / 注册</view>
      <view class="privacy-link" @click="go('/pages/privacy/privacy')">查看用户协议与隐私政策</view>
    </view>

    <!-- 首次启动强制同意；不同意则无法使用（合规要求） -->
    <PrivacyConsent v-if="showConsent" @agree="onAgreePrivacy" @disagree="onDisagreePrivacy" />
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import PrivacyConsent from '../../components/PrivacyConsent.vue'
import { authApi, userApi } from '../../utils/api.js'
import { saveProfile, maskPhone } from '../../utils/store.js'
import { setToken, getToken, clearToken } from '../../utils/request.js'
import { setOfflineScope } from '../../utils/offline.js'
import { hasAgreedPrivacy, agreePrivacy } from '../../utils/privacy.js'
import { quitApp } from '../../utils/platform.js'

export default {
  components: { GIcon, PrivacyConsent },
  data() {
    return {
      phone: '',
      code: '',
      notice: '',
      canBack: false,
      sending: false,
      submitting: false,
      // 未同意协议前，弹窗常驻，阻止一切接口调用
      showConsent: false
    }
  },
  onLoad() {
    this.canBack = getCurrentPages().length > 1
    this.showConsent = !hasAgreedPrivacy()
    // 已登录过就直接进首页：短信验证码要花钱，不能每次冷启动都逼用户重新登录
    this.tryAutoLogin()
  },
  methods: {
    /**
     * 免登录进入。
     *
     * 本地有 token 时校验一次：有效则直接进首页；已失效(401)才回登录页。
     * 注意「网络异常」不能当成失效 —— 本应用是离线优先的，断网时也要能进首页用本地数据。
     */
    async tryAutoLogin() {
      if (!getToken()) return
      try {
        await userApi.getProfile()
        uni.reLaunch({ url: '/pages/index/index' })
      } catch (err) {
        if (err && err.code === 401) {
          // token 真的失效了：清掉，留在登录页
          clearToken()
          return
        }
        // 网络不可用等其它情况：保持登录状态，直接进首页（走本地数据）
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },
    goBack() {
      uni.navigateBack()
    },
    go(url) {
      uni.navigateTo({ url })
    },
    onAgreePrivacy() {
      agreePrivacy()
      this.showConsent = false
      uni.showToast({ title: '已同意，可以开始使用', icon: 'none' })
    },
    onDisagreePrivacy() {
      if (quitApp()) return
      // H5 / 小程序无法主动关闭，明确告知并保持弹窗，无法继续
      uni.showToast({ title: '需同意协议后才能使用，可直接关闭页面', icon: 'none' })
    },
    sendCode() {
      if (!hasAgreedPrivacy()) {
        this.showConsent = true
        return
      }
      if (!/^1\d{10}$/.test(this.phone)) {
        this.notice = '请输入正确的 11 位手机号'
        return
      }
      if (this.sending) return
      this.sending = true
      this.notice = ''
      authApi.sendSmsCode({
        phone: this.phone,
        scene: 'login'
      }).then(() => {
        this.notice = '验证码已发送至 ' + maskPhone(this.phone)
      }).catch((err) => {
        this.notice = (err && err.message) || '验证码发送失败'
      }).finally(() => {
        this.sending = false
      })
    },
    submit() {
      if (!hasAgreedPrivacy()) {
        this.showConsent = true
        return
      }
      if (!/^1\d{10}$/.test(this.phone)) {
        this.notice = '请输入正确的 11 位手机号'
        return
      }
      // 验证码位数不写死：不同短信服务商可能发 4~8 位
      if (!/^\d{4,8}$/.test(String(this.code).trim())) {
        this.notice = '请输入收到的验证码'
        return
      }
      if (this.submitting) return
      this.submitting = true
      this.notice = ''

      authApi.login({
        phone: this.phone,
        code: this.code
      }).then((res) => {
        setToken(res.token)
        // 切换离线数据作用域到当前用户，避免看到上一个账号的待同步记录
        setOfflineScope(res.userInfo && res.userInfo.id)
        saveProfile({
          nickname: res.userInfo.nickname,
          phone: maskPhone(res.userInfo.phone),
          avatar: res.userInfo.avatar || ''
        })
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 400)
      }).catch((err) => {
        this.notice = (err && err.message) || '登录失败，请稍后重试'
      }).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.auth-card {
  margin-top: 40rpx;
}

.auth-tip {
  font-size: 24rpx;
  color: var(--ggg-text-3);
  text-align: center;
  margin-bottom: 8rpx;
}

.field-item {
  margin-top: 24rpx;
  background: var(--ggg-surface);
  border: 2rpx solid var(--ggg-border-2);
  border-radius: 32rpx;
  padding: 16rpx 24rpx;
}

.field-label {
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.field-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  font-size: 32rpx;
  margin-top: 8rpx;
  height: 56rpx;
  line-height: 56rpx;
  color: var(--ggg-text-1);
}

.code-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.code-input {
  flex: 1;
  min-width: 0;
}

.code-btn {
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 24rpx;
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  flex: none;
}

.notice {
  margin-top: 20rpx;
  color: var(--ggg-accent);
  font-size: 24rpx;
}

.submit-btn {
  margin-top: 32rpx;
}

.privacy-link {
  margin-top: 20rpx;
  text-align: center;
  color: var(--ggg-terra);
  font-size: 28rpx;
}
</style>
