<template>
  <view class="g-icon" :style="iconStyle">
  <!-- #ifdef H5 -->
    <iconify-icon v-if="solarName" :icon="solarName"></iconify-icon>
    <text v-else>{{ symbol }}</text>
  <!-- #endif -->
  <!-- #ifndef H5 -->
    <text>{{ symbol }}</text>
  <!-- #endif -->
  </view>
</template>

<script>
const icons = {
  home: '⌂',
  chart: '◔',
  settings: '⚙',
  calendar: '▤',
  plus: '＋',
  check: '✓',
  restart: '↻',
  recycle: '↺',
  trash: '✕',
  user: '◉',
  edit: '✎',
  back: '‹',
  arrow: '›',
  export: '⇪',
  shield: '◈',
  bell: '◷',
  tag: '☰',
  text: 'Aa',
  palette: '◐',
  backup: '↑',
  info: 'i',
  feedback: '✉',
  send: '→',
  lock: '◇',
  download: '↓',
  save: '✓',
  logout: '⏻'
}

const solarIcons = {
  home: 'solar:home-smile-angle-bold',
  chart: 'solar:chart-2-bold',
  settings: 'solar:settings-bold',
  calendar: 'solar:calendar-minimalistic-linear',
  plus: 'solar:add-circle-bold',
  check: 'solar:check-read-linear',
  restart: 'solar:restart-circle-linear',
  recycle: 'solar:recycle-linear',
  trash: 'solar:trash-bin-minimalistic-linear',
  user: 'solar:user-circle-bold',
  edit: 'solar:pen-new-square-linear',
  back: 'solar:alt-arrow-left-linear',
  arrow: 'solar:alt-arrow-right-linear',
  export: 'solar:export-bold',
  shield: 'solar:shield-check-bold',
  bell: 'solar:bell-linear',
  tag: 'solar:tag-horizontal-linear',
  text: 'solar:text-field-focus-linear',
  palette: 'solar:palette-linear',
  backup: 'solar:cloud-upload-linear',
  info: 'solar:info-circle-linear',
  feedback: 'solar:chat-round-call-linear',
  lock: 'solar:lock-password-linear',
  download: 'solar:download-minimalistic-bold',
  save: 'solar:check-read-bold',
  send: 'solar:plain-bold',
  logout: 'solar:logout-2-linear'
}

export default {
  name: 'GIcon',
  props: {
    name: {
      type: String,
      default: 'check'
    },
    size: {
      type: [Number, String],
      default: 32
    },
    color: {
      type: String,
      default: ''
    }
  },
  computed: {
    symbol() {
      return icons[this.name] || this.name
    },
    solarName() {
      return solarIcons[this.name] || ''
    },
    iconStyle() {
      return {
        fontSize: this.size + 'rpx',
        width: this.size + 'rpx',
        height: this.size + 'rpx',
        lineHeight: this.size + 'rpx',
        color: this.color || undefined
      }
    }
  }
}
</script>

<style scoped>
.g-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-weight: 700;
}
</style>
