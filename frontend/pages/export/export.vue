<template>
  <ThemePage>
    <view class="ggg-header">
      <view class="ggg-back" @click="goBack"><GIcon name="back" :size="34" color="var(--ggg-terra)" /></view>
      <view class="ggg-title">数据导出</view>
      <view class="ggg-back"><GIcon name="export" :size="30" color="var(--ggg-accent)" /></view>
    </view>

    <view class="ggg-card">
      <view class="export-title">数据归你，安心留存</view>
      <view class="ggg-muted ggg-small">可导出全部记录、分类与用户资料</view>
      <view class="export-grid">
        <view class="export-item" @click="exportJSON">
          <view class="export-mark"><GIcon name="download" :size="34" color="var(--ggg-accent)" /></view>
          <view>导出 JSON</view>
        </view>
        <view class="export-item" @click="exportCSV">
          <view class="export-mark"><GIcon name="export" :size="34" color="var(--ggg-accent)" /></view>
          <view>导出 CSV</view>
        </view>
      </view>
      <view class="ggg-btn ggg-btn-ghost copy-btn" @click="copyCSV">
        <GIcon name="save" :size="26" color="var(--ggg-brown)" /> 复制 CSV 到剪贴板
      </view>
      <view class="ggg-muted ggg-small export-note">{{ outputNote }}</view>
    </view>

    <view class="ggg-card privacy-card">
      <view class="privacy-title"><GIcon name="shield" :size="30" color="var(--ggg-accent)" /> 本地存储与隐私</view>
      <view class="ggg-muted ggg-small">默认记录仅保存在本机，未经允许不上传</view>
    </view>

    <view class="ggg-btn clear-btn" @click="clearAll"><GIcon name="trash" :size="26" color="var(--ggg-terra)" /> 一键清空数据</view>
  </ThemePage>
</template>

<script>
import GIcon from '../../components/GIcon.vue'
import { dataApi } from '../../utils/api.js'
import { explainPermission, hasShownPermission } from '../../utils/permission.js'
import { isH5, isApp } from '../../utils/platform.js'
import { buildCsv, buildJson } from '../../utils/exportBuild.js'

export default {
  components: { GIcon },
  computed: {
    outputNote() {
      if (isH5()) return '浏览器中会直接下载文件'
      if (isApp()) return 'App 端会调起系统分享面板发送文件（需已安装 life-FileShare 原生插件）；插件缺失时自动改为发送文本内容'
      return '当前环境会复制到剪贴板，可粘贴到备忘录等应用中保存'
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    // 合规要求：导出前说明用途与数据去向；仅首次说明
    async ensureExportExplained() {
      if (hasShownPermission('export')) return true
      return explainPermission('export')
    },
    // categoryId -> 分类名。后端导出结果里带了 categories，用它把名字补上
    async withExportData(fn) {
      if (!(await this.ensureExportExplained())) return
      try {
        const data = await dataApi.export()
        await fn(data || {})
      } catch (err) {
        uni.showToast({ title: (err && err.message) || '导出失败', icon: 'none' })
      }
    },
    exportJSON() {
      this.withExportData(async (data) => {
        await this.output(buildJson(data), 'gongguoge-export.json')
      })
    },
    exportCSV() {
      this.withExportData(async (data) => {
        await this.output(buildCsv(data), 'gongguoge-export.csv')
      })
    },
    // 复制 CSV 到剪贴板：内容很长、或分享面板接收不全时的可靠通道
    copyCSV() {
      this.withExportData(async (data) => {
        this.copyToClipboard(buildCsv(data))
      })
    },
    // 按平台选择输出方式：H5 下载文件、App 保存并分享、其余复制到剪贴板
    async output(content, filename) {
      if (isH5()) {
        this.downloadInBrowser(content, filename)
        return
      }
      if (isApp()) {
        this.saveAndShareOnApp(content, filename)
        return
      }
      this.copyToClipboard(content)
    },
    downloadInBrowser(content, filename) {
      try {
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        document.body.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)
        uni.showToast({ title: '已下载 ' + filename, icon: 'none' })
      } catch (e) {
        this.copyToClipboard(content)
      }
    },
    /**
     * App 端导出。
     *
     * 两条路，按可用性自动选择：
     *   1. **原生插件 life-FileShare**：真正把文件通过系统分享面板发出去（微信/QQ 能收到 .csv 文件）
     *   2. 插件未安装时 → 退回**文本分享**（plus.share.ShareMessage 的 content 字段，官方支持）
     *
     * 背景（查官方规范确认）：plus.share.ShareMessage 只支持 content/pictures/href/title，
     * uni.share 的 type 也只有图文/文字/图片/音乐/视频/小程序——**都不支持任意文件**。
     * 所以「发文件」必须依赖原生插件。
     */
    saveAndShareOnApp(content, filename) {
      this.writeAppFile(content, filename).then((fsPath) => {
        // 有插件就走文件分享；没有/调起失败就退回文本分享
        if (fsPath && this.shareFileByPlugin(fsPath, filename)) return
        this.shareAsText(content)
      })
    },
    /**
     * 用原生插件分享文件。
     *
     * 依赖：HBuilderX → manifest.json → App原生插件配置 → 勾选 life-FileShare
     *       （免费插件，需登录 DCloud 账号绑定到本项目；真机调试需打自定义基座）
     * 返回 true 表示已成功调起系统分享面板。
     */
    shareFileByPlugin(fsPath, filename) {
      try {
        if (typeof uni.requireNativePlugin !== 'function') return false
        const FileShare = uni.requireNativePlugin('life-FileShare')
        if (!FileShare || typeof FileShare.render !== 'function') return false
        // 插件要求传「真实文件系统路径」，且不带 file:// 协议
        const path = String(fsPath).replace(/^file:\/\//, '')
        FileShare.render({
          type: 'SYSTEM',   // SYSTEM = 系统分享面板；WX/QQ 需要另外配置开放平台 appid
          filePath: path
        }, () => {
          // 插件回调只表示"已调起面板"，无法得知用户是否真的发送
        })
        return true
      } catch (e) {
        return false
      }
    },
    // 写入应用私有目录，返回真实文件系统路径（插件需要）；失败返回空串
    writeAppFile(content, filename) {
      return new Promise((resolve) => {
        try {
          plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
            fs.root.getFile(filename, { create: true }, (entry) => {
              entry.createWriter((writer) => {
                writer.onwrite = () => {
                  let path = ''
                  try {
                    // toURL() 返回 _doc/xxx.csv，这个转换才是插件要的绝对路径
                    path = plus.io.convertLocalFileSystemURL(entry.toURL())
                  } catch (e) {
                    path = String(entry.toLocalURL()).replace(/^file:\/\//, '')
                  }
                  resolve(path)
                }
                writer.onerror = () => resolve('')
                writer.write(content)
              }, () => resolve(''))
            }, () => resolve(''))
          }, () => resolve(''))
        } catch (e) {
          resolve('')
        }
      })
    },
    // 以「文本」形式分享（插件不可用时的兜底；content 是官方支持的字段）
    shareAsText(content) {
      const tooLong = String(content).length > 4000
      try {
        if (typeof plus !== 'undefined' && plus.share && typeof plus.share.sendWithSystem === 'function') {
          plus.share.sendWithSystem(
            { content: content, title: '功过格数据导出' },
            () => {
              if (tooLong) {
                uni.showToast({ title: '内容较长，如接收不全可用「复制」', icon: 'none', duration: 2500 })
              }
            },
            () => this.copyToClipboard(content)
          )
          return
        }
      } catch (e) {
        // 落到剪贴板
      }
      this.copyToClipboard(content)
    },
    copyToClipboard(content) {
      uni.setClipboardData({
        data: content,
        success: () => uni.showToast({ title: '内容已复制到剪贴板', icon: 'none' })
      })
    },
    clearAll() {
      uni.showModal({
        title: '确认清空',
        content: '确认清空全部记录与回收站数据？此操作不可撤销。',
        success: (res) => {
          if (!res.confirm) return
          dataApi.clear().then(() => {
            uni.showToast({ title: '已清空', icon: 'success' })
          }).catch((err) => {
            uni.showToast({ title: (err && err.message) || '清空失败', icon: 'none' })
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.ggg-card {
  margin-top: 28rpx;
}

.export-title,
.privacy-title {
  font-size: 32rpx;
  font-weight: 700;
}

.export-grid {
  display: flex;
  gap: 20rpx;
  margin-top: 28rpx;
}

.export-note {
  margin-top: 22rpx;
  line-height: 1.6;
}

.copy-btn {
  margin-top: 24rpx;
  border: 1rpx solid var(--ggg-border-ghost);
  color: var(--ggg-brown);
  background: transparent;
  gap: 8rpx;
}

.export-item {
  flex: 1;
  height: 160rpx;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: var(--ggg-accent-soft);
  color: var(--ggg-accent);
  font-weight: 600;
}

.export-item:first-child {
  background: transparent;
  border: 2rpx solid var(--ggg-border-ghost);
  color: var(--ggg-brown);
}

.export-item:first-child .export-mark {
  color: var(--ggg-brown);
}


.export-mark {
  font-size: 30rpx;
  font-weight: 700;
}

.privacy-card {
  margin-top: 24rpx;
}

.privacy-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.clear-btn {
  margin-top: 24rpx;
  border: 1rpx solid var(--ggg-terra-deep);
  color: var(--ggg-terra);
  background: transparent;
  gap: 8rpx;
}
</style>
