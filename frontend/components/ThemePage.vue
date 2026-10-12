<template>
  <view class="ggg-page" :class="[themeClass, fontClass]" :style="fontStyle">
    <slot />
  </view>
</template>

<script>
import { loadSavedAppearance, themeClass, fontClass, fontStyle, applyAppearance } from '../utils/appearance.js'

export default {
  name: 'ThemePage',
  data() {
    return {
      appearance: loadSavedAppearance()
    }
  },
  computed: {
    themeClass() {
      return themeClass(this.appearance)
    },
    fontClass() {
      return fontClass(this.appearance)
    },
    fontStyle() {
      return fontStyle(this.appearance)
    }
  },
  created() {
    applyAppearance(this.appearance)
    uni.$on('appearanceChange', this.onAppearanceChange)
  },
  beforeDestroy() {
    uni.$off('appearanceChange', this.onAppearanceChange)
  },
  methods: {
    onAppearanceChange(appearance) {
      this.appearance = appearance
      applyAppearance(appearance)
    }
  }
}
</script>
