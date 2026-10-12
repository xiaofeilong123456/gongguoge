<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">编辑资料</view>
      <view class="ggg-back"><GIcon name="edit" :size="30" color="var(--ggg-accent-strong)" /></view>
    </view>

    <view class="avatar-wrap">
      <view class="profile-avatar" v-if="avatarSrc">
        <image class="avatar-image" :src="avatarSrc" mode="aspectFill" @error="onAvatarError" @load="onAvatarLoad"></image>
      </view>
      <view class="profile-avatar default-avatar" v-else><GIcon name="user" :size="72" color="var(--ggg-accent-strong)" /></view>
      <view class="avatar-btn" @click="chooseAvatar">
        <GIcon name="edit" :size="26" color="var(--ggg-accent-strong)" />
        <text>{{ uploading ? '上传中…' : (avatarSrc ? '更换头像' : '上传头像') }}</text>
      </view>
      <view class="ggg-muted avatar-hint">支持 jpg / png / webp / gif，不超过 2MB</view>
      <!-- 加载失败时把实际请求地址显示出来，便于判断是网络还是地址问题 -->
      <view v-if="avatarFailed" class="avatar-error">
        <text>头像加载失败，实际请求地址：</text>
        <text class="avatar-error-url">{{ avatarUrl }}</text>
      </view>
    </view>

    <view class="ggg-card form-card">
      <view class="form-item">
        <view class="form-label">昵称</view>
        <input class="ggg-input" v-model="nickname" placeholder="请输入昵称" />
      </view>
      <view class="form-item">
        <view class="form-label">手机号</view>
        <input class="ggg-input" type="number" maxlength="11" v-model="phone" />
      </view>
      <view class="ggg-btn ggg-btn-primary save-btn" @click="save">保存资料</view>
    </view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { userApi, fileApi } from '../../utils/api.js'
import { saveProfile } from '../../utils/store.js'
import { toAbsoluteUrl } from '../../utils/request.js'
import { resolveImageSrc, clearImageCache } from '../../utils/image.js'

export default {
  components: { GIcon },
  data() {
    return {
      profile: {},
      nickname: '',
      phone: '',
      uploading: false,
      avatarFailed: false,
      // 实际用于 <image src> 的值：小程序端可能是 base64 data URI
      avatarSrc: ''
    }
  },
  computed: {
    // 数据库里存的是相对路径（如 /api/files/avatar/...），用于展示与诊断
    avatarUrl() {
      return toAbsoluteUrl(this.profile.avatar)
    }
  },
  onShow() {
    this.loadProfile()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
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
        this.avatarSrc = src
        if (!src) {
          // 转换失败：退回直接使用原地址，并让 @error 给出诊断
          this.avatarSrc = this.avatarUrl
        }
      })
    },
    loadProfile() {
      userApi.getProfile().then((res) => {
        this.profile = res || {}
        this.nickname = res.nickname || ''
        this.phone = res.phone || ''
        this.loadAvatarSrc()
      }).catch(() => {
        const local = uni.getStorageSync('ggg_profile') || {}
        this.profile = local
        this.nickname = local.nickname || ''
        this.phone = local.phone || ''
        this.loadAvatarSrc()
      })
    },
    chooseAvatar() {
      if (this.uploading) return
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const tempFilePath = res.tempFilePaths && res.tempFilePaths[0]
          if (tempFilePath) this.uploadAvatar(tempFilePath)
        }
      })
    },
    // 先上传拿到服务端 URL，再写入表单；只存本地临时路径在换设备后会失效
    async uploadAvatar(filePath) {
      this.uploading = true
      uni.showLoading({ title: '上传中' })
      try {
        const res = await fileApi.upload(filePath)
        const url = (res && res.url) || ''
        if (!url) throw new Error('服务端未返回文件地址')
        this.profile = Object.assign({}, this.profile, { avatar: url })
        // 换头像后清掉图片缓存，并重新解析实际地址
        clearImageCache()
        this.loadAvatarSrc()
        uni.showToast({ title: '头像已上传，记得保存', icon: 'none', duration: 2000 })
      } catch (err) {
        uni.showToast({ title: (err && err.message) || '头像上传失败', icon: 'none' })
      } finally {
        this.uploading = false
        uni.hideLoading()
      }
    },
    save() {
      userApi.updateProfile({
        nickname: this.nickname.trim() || '功过格用户',
        // 存服务端返回的相对路径，避免把 http://localhost 这类地址写进库
        avatar: this.profile.avatar || ''
      }).then((res) => {
        saveProfile({
          nickname: res.nickname,
          phone: res.phone,
          avatar: res.avatar
        })
        uni.showToast({ title: '已保存', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 400)
      }).catch((err) => {
        uni.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
      })
    }
  }
}
</script>

<style scoped>
.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 36rpx;
}

.profile-avatar {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.default-avatar {
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent-strong);
  font-size: 56rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-btn {
  margin-top: 20rpx;
  color: var(--ggg-accent-strong);
  font-size: 26rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.avatar-hint {
  margin-top: 8rpx;
}

.avatar-error {
  margin-top: 16rpx;
  padding: 14rpx 18rpx;
  border-radius: 16rpx;
  background: var(--ggg-terra-soft);
  color: var(--ggg-error-text);
  font-size: 22rpx;
  line-height: 1.6;
  text-align: center;
  max-width: 560rpx;
}

.avatar-error-url {
  display: block;
  margin-top: 6rpx;
  word-break: break-all;
  font-size: 20rpx;
}

.form-card {
  margin-top: 36rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 24rpx;
  color: var(--ggg-text-3);
  margin-bottom: 12rpx;
}

.save-btn {
  margin-top: 8rpx;
}
</style>
