<template>
  <ThemePage>
    <view class="ggg-header">
      <view>
        <view class="ggg-title">设置</view>
      </view>
    </view>

    <view class="ggg-card profile-card" @click="goProfile">
      <view class="profile-avatar" v-if="avatarSrc">
        <image class="avatar-image" :src="avatarSrc" mode="aspectFill" @error="onAvatarError" @load="onAvatarLoad"></image>
      </view>
      <view class="profile-avatar default-avatar" v-else><GIcon name="user" :size="56" color="var(--ggg-accent-strong)" /></view>
      <view class="profile-main">
        <view class="profile-name">{{ profile.nickname }}</view>
        <view class="profile-phone">{{ profile.phone }}</view>
        <view v-if="avatarFailed" class="avatar-error-url">{{ avatarUrl }}</view>
      </view>
      <view class="profile-edit"><GIcon name="edit" :size="32" color="var(--ggg-accent-strong)" /></view>
    </view>

    <view class="ggg-card group-card">
      <view class="ggg-row" @click="go('/pages/reminder/reminder')">
        <view class="row-icon"><GIcon name="bell" :size="28" color="var(--ggg-accent)" /></view><view class="row-label">提醒设置</view><view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/categories/categories')">
        <view class="row-icon"><GIcon name="tag" :size="28" color="var(--ggg-accent)" /></view><view class="row-label">标签管理</view><view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/appearance/appearance')">
        <view class="row-icon"><GIcon name="palette" :size="28" color="var(--ggg-accent)" /></view>
        <view class="row-main"><view class="row-label">页面设置</view><view class="row-sub">字体大小与显示</view></view>
        <view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/password/password')">
        <view class="row-icon"><GIcon name="lock" :size="28" color="var(--ggg-accent)" /></view>
        <view class="row-main"><view class="row-label">修改密码</view><view class="row-sub">设置或更换登录密码</view></view>
        <view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
    </view>

    <view class="section-title">数据与隐私</view>
    <view class="ggg-card group-card">
      <view class="ggg-row" @click="go('/pages/recycle/recycle')">
        <view class="row-icon"><GIcon name="recycle" :size="28" color="var(--ggg-accent)" /></view>
        <view class="row-main"><view class="row-label">回收站</view><view class="row-sub">删除记录 7 天可恢复</view></view>
        <view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/storage/storage')">
        <view class="row-icon"><GIcon name="shield" :size="28" color="var(--ggg-accent)" /></view>
        <view class="row-main"><view class="row-label">本地存储与隐私</view><view class="row-sub">记录默认仅保存在本机</view></view>
        <view class="row-arrow"><GIcon name="arrow" :size="28" color="#aaa79f" /></view>
      </view>
    </view>

    <view class="ggg-card group-card">
      <view class="ggg-row" @click="go('/pages/privacy/privacy')">
        <view class="row-icon"><GIcon name="shield" :size="28" color="var(--ggg-accent)" /></view><view class="row-main"><view class="row-label">用户协议与隐私政策</view><view class="row-sub">查看完整条款</view></view><view class="row-arrow"><GIcon name="arrow" :size="28" color="var(--ggg-text-2)" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/permissions/permissions')">
        <view class="row-icon"><GIcon name="lock" :size="28" color="var(--ggg-accent)" /></view><view class="row-main"><view class="row-label">权限管理</view><view class="row-sub">查看每项权限的用途</view></view><view class="row-arrow"><GIcon name="arrow" :size="28" color="var(--ggg-text-2)" /></view>
      </view>
      <view class="ggg-row" @click="withdrawConsent">
        <view class="row-icon"><GIcon name="restart" :size="28" color="var(--ggg-terra)" /></view><view class="row-main"><view class="row-label">撤回协议同意</view><view class="row-sub">撤回后重新展示同意弹窗</view></view><view class="row-arrow"><GIcon name="arrow" :size="28" color="var(--ggg-text-2)" /></view>
      </view>
    </view>

    <view class="ggg-card group-card">
      <view class="ggg-row" @click="go('/pages/about/about')">
        <view class="row-icon"><GIcon name="info" :size="28" color="var(--ggg-accent)" /></view><view class="row-label">关于我们</view><view class="row-arrow"><GIcon name="arrow" :size="28" color="var(--ggg-text-2)" /></view>
      </view>
      <view class="ggg-row" @click="go('/pages/feedback/feedback')">
        <view class="row-icon"><GIcon name="feedback" :size="28" color="var(--ggg-accent)" /></view><view class="row-label">意见反馈</view><view class="row-arrow"><GIcon name="arrow" :size="28" color="var(--ggg-text-2)" /></view>
      </view>
    </view>

    <!-- <view class="assets-btn" @click="assetsVisible = !assetsVisible">
      <view v-if="!assetsVisible">查看 App 图标与商店截图预览</view>
      <view v-else>收起商店素材</view>
    </view>
    <view class="assets-wrap" v-if="assetsVisible">
      <view class="ggg-card asset-card">
        <view class="asset-title">小尺寸 App 图标适配</view>
        <view class="asset-icons">
          <view class="app-icon-large">
            <view class="app-icon-ring"></view>
            <text class="app-icon-text">功过<br />格</text>
            <text class="app-icon-badge">省</text>
          </view>
          <view class="app-icon-small">
            <view class="app-icon-small-ring"></view>
            <text class="app-icon-small-text">功过<br />格</text>
          </view>
        </view>
        <view class="ggg-muted ggg-small">120×120 / 40×40</view>
      </view>
      <view class="ggg-card store-shot">
        <view class="store-title">数据归你，安心留存</view>
        <view class="ggg-muted ggg-small">第 5 张商店截图预览</view>
        <view class="store-row"><GIcon name="export" :size="28" color="var(--ggg-accent)" /><view><view class="store-name">随时导出</view><view class="store-desc">CSV 文件清晰可读，记录不被锁定</view></view></view>
        <view class="store-row"><GIcon name="shield" :size="28" color="var(--ggg-accent)" /><view><view class="store-name">本地存储</view><view class="store-desc">默认仅保存在本机，未经允许不上传</view></view></view>
      </view>
    </view> -->

    <view class="logout-wrap">
      <view class="ggg-btn ggg-btn-ghost logout-btn" @click="confirmLogout">
        <GIcon name="logout" :size="30" color="var(--ggg-terra-strong)" />
        <text>{{ loggingOut ? '正在退出…' : '退出登录' }}</text>
      </view>
      <view class="ggg-muted ggg-small logout-hint">退出后需要重新用手机号登录</view>
      <view class="danger-link" @click="confirmDeleteAccount">
        {{ deletingAccount ? '正在注销…' : '注销账号' }}
      </view>
    </view>

    <PageNav current="settings" />
  </ThemePage>
</template>

<script>
import PageNav from '../../components/PageNav.vue'
import GIcon from '../../components/GIcon.vue'
import { userApi, authApi } from '../../utils/api.js'
import { getProfile, clearProfile } from '../../utils/store.js'
import { resetPrivacy } from '../../utils/privacy.js'
import { clearToken, toAbsoluteUrl } from '../../utils/request.js'
import { clearOfflineScope, queueSize } from '../../utils/offline.js'
import { resolveImageSrc } from '../../utils/image.js'
import { flushQueue } from '../../utils/sync.js'

export default {
  components: { PageNav, GIcon },
  data() {
    return {
      profile: {},
      assetsVisible: false,
      avatarFailed: false,
      loggingOut: false,
      deletingAccount: false,
      // 实际用于 <image src> 的值：小程序端可能是 base64 data URI
      avatarSrc: ''
    }
  },
  computed: {
    // 数据库存相对路径，用于展示与诊断
    avatarUrl() {
      return toAbsoluteUrl(this.profile.avatar)
    }
  },
  onShow() {
    this.loadProfile()
  },
  methods: {
    onAvatarError(e) {
      this.avatarFailed = true
      console.warn('[avatar] 加载失败，请求地址 =', this.avatarUrl, e)
    },
    onAvatarLoad() {
      this.avatarFailed = false
    },
    // 解析出实际可用的图片地址（小程序端 http 图片需转 base64）
    loadAvatarSrc() {
      const path = this.profile.avatar
      if (!path) {
        this.avatarSrc = ''
        return
      }
      resolveImageSrc(path).then((src) => {
        this.avatarSrc = src || this.avatarUrl
      })
    },
    loadProfile() {
      userApi.getProfile().then((res) => {
        this.profile = res || {}
        this.loadAvatarSrc()
      }).catch(() => {
        this.profile = getProfile()
        this.loadAvatarSrc()
      })
    },
    goProfile() {
      uni.navigateTo({ url: '/pages/profile/profile' })
    },
    withdrawConsent() {
      uni.showModal({
        title: '撤回协议同意',
        content: '撤回后需重新阅读并同意《用户协议》与《隐私政策》才能继续使用。是否继续？',
        confirmText: '撤回',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return
          resetPrivacy()
          clearToken()
          clearOfflineScope()
          clearProfile()
          uni.reLaunch({ url: '/pages/auth/auth' })
        }
      })
    },
    // 退出登录：先给一次机会把待同步记录提交上去，避免退出丢数据
    confirmLogout() {
      if (this.loggingOut) return
      const pending = queueSize()
      const content = pending > 0
        ? '本机还有 ' + pending + ' 条记录未同步，退出后会丢失。确定退出？'
        : '退出后需要重新用手机号登录，确定退出？'
      uni.showModal({
        title: '退出登录',
        content: content,
        confirmText: '退出',
        confirmColor: '#b8663e',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return
          this.doLogout()
        }
      })
    },
    async doLogout() {
      this.loggingOut = true
      uni.showLoading({ title: '正在退出' })
      // 尽力提交未同步记录；失败也不阻塞退出
      try {
        await flushQueue()
      } catch (e) {
        // 忽略：离线也要能退出
      }
      try {
        await authApi.logout()
      } catch (e) {
        // 服务端退出失败不影响本地退出
      }
      clearToken()
      clearOfflineScope()
      clearProfile()
      uni.hideLoading()
      this.loggingOut = false
      uni.reLaunch({ url: '/pages/auth/auth' })
    },
    // 注销账号：不可恢复，做两次确认
    confirmDeleteAccount() {
      if (this.deletingAccount || this.loggingOut) return
      const pending = queueSize()
      const extra = pending > 0 ? '\n\n本机还有 ' + pending + ' 条记录未同步，注销后会一并丢弃。' : ''
      uni.showModal({
        title: '注销账号',
        content: '将永久删除你的账号，以及全部记录、标签、回收站数据与反馈。此操作不可恢复。' + extra,
        confirmText: '继续',
        confirmColor: '#b8663e',
        cancelText: '取消',
        success: (res) => {
          if (!res.confirm) return
          uni.showModal({
            title: '再次确认',
            content: '注销后无法找回任何数据，确定要注销吗？',
            confirmText: '确认注销',
            confirmColor: '#b8663e',
            cancelText: '我再想想',
            success: (res2) => {
              if (!res2.confirm) return
              this.doDeleteAccount()
            }
          })
        }
      })
    },
    async doDeleteAccount() {
      this.deletingAccount = true
      uni.showLoading({ title: '正在注销', mask: true })
      try {
        await userApi.deleteAccount()
      } catch (e) {
        // 服务端删除失败时不清理本地：避免「本地清了、服务端还在」的不一致
        uni.hideLoading()
        this.deletingAccount = false
        uni.showModal({
          title: '注销失败',
          content: (e && e.message) || '请检查网络后重试',
          showCancel: false
        })
        return
      }
      // 服务端已删除，本地状态一并清空
      resetPrivacy()
      clearToken()
      clearOfflineScope()
      clearProfile()
      uni.hideLoading()
      this.deletingAccount = false
      uni.showToast({ title: '账号已注销', icon: 'success', duration: 1500 })
      setTimeout(() => uni.reLaunch({ url: '/pages/auth/auth' }), 900)
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style scoped>
.profile-card {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.profile-avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  overflow: hidden;
  flex: none;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-error-url {
  margin-top: 6rpx;
  font-size: 18rpx;
  color: var(--ggg-error-text);
  word-break: break-all;
  line-height: 1.4;
}

.default-avatar {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent-strong);
  font-size: 40rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 32rpx;
  font-weight: 700;
}

.profile-phone {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--ggg-text-2);
}

.profile-edit {
  color: var(--ggg-accent-strong);
  font-size: 40rpx;
}

.group-card {
  margin-top: 24rpx;
  padding: 0;
  overflow: hidden;
}

.section-title {
  margin: 36rpx 4rpx 16rpx;
  font-weight: 700;
}

.row-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 24rpx;
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.row-label {
  flex: 1;
  margin-left: 24rpx;
  font-size: 32rpx;
}

.row-main {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
}

.row-main .row-label {
  margin-left: 0;
}

.row-sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--ggg-text-3);
}

.row-arrow {
  color: #aaa79f;
  font-size: 44rpx;
}

.assets-btn {
  margin-top: 28rpx;
  height: 88rpx;
  border: 1rpx solid var(--ggg-border-ghost);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ggg-brown);
  font-weight: 600;
}

.assets-wrap {
  margin-top: 20rpx;
}

.asset-title,
.store-title {
  font-size: 30rpx;
  font-weight: 700;
}

.asset-icons {
  display: flex;
  align-items: flex-end;
  gap: 32rpx;
  margin: 24rpx 0;
}

.app-icon-large,
.app-icon-small {
  position: relative;
  background: var(--ggg-surface);
  border: 1rpx solid #ddd8ce;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.app-icon-large {
  width: 180rpx;
  height: 180rpx;
  border-radius: 40rpx;
}

.app-icon-small {
  width: 60rpx;
  height: 60rpx;
  border-radius: 14rpx;
}

.app-icon-ring {
  position: absolute;
  width: 144rpx;
  height: 144rpx;
  border: 12rpx solid rgba(0, 0, 0, 0.2);
  border-radius: 50%;
}

.app-icon-small-ring {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border: 6rpx solid rgba(0, 0, 0, 0.2);
  border-radius: 50%;
}

.app-icon-text {
  position: relative;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.2;
}

.app-icon-small-text {
  position: relative;
  font-size: 14rpx;
  font-weight: 800;
  line-height: 1.2;
}

.app-icon-badge {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  background: var(--ggg-terra-deep);
  color: #ffffff;
  font-size: 16rpx;
  padding: 4rpx 8rpx;
  border-radius: 4rpx;
}

.store-shot {
  margin-top: 20rpx;
}

.store-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
  padding: 20rpx;
  background: var(--ggg-surface-card);
  border-radius: 20rpx;
}

.store-name {
  font-weight: 600;
}

.store-desc {
  margin-top: 4rpx;
  color: var(--ggg-text-3);
  font-size: 22rpx;
}

.logout-wrap {
  margin-top: 44rpx;
  /* 给底部导航栏让出空间 */
  padding-bottom: 160rpx;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 96rpx;
  border-radius: 28rpx;
  border: 2rpx solid var(--ggg-terra);
  color: var(--ggg-terra-strong);
  font-size: 30rpx;
  font-weight: 600;
  background: transparent;
}

.logout-hint {
  margin-top: 16rpx;
  text-align: center;
}

/* 注销账号：不可逆操作，做成低调的文字链，避免误触 */
.danger-link {
  margin-top: 36rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--ggg-text-3);
  text-decoration: underline;
  padding: 16rpx;
}
</style>
