export function isDark(appearance) {
  const theme = appearance.theme || 'system'
  if (theme === 'dark') return true
  if (theme === 'light') return false
  if (typeof uni !== 'undefined' && uni.getSystemInfoSync && uni.getSystemInfoSync().theme === 'dark') return true
  return typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
}

export function themeClass(appearance) {
  return isDark(appearance) ? 'theme-dark' : 'theme-light'
}

export function fontClass(appearance) {
  return 'font-' + (appearance.fontSize || 'medium')
}

export function fontStyle(appearance) {
  const size = appearance.fontSize || 'medium'
  return {
    fontSize: size === 'large' ? '18px' : size === 'small' ? '14px' : '16px'
  }
}

export function applyAppearance(appearance) {
  const dark = isDark(appearance)
  // #ifdef H5
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
  document.documentElement.style.fontSize = fontStyle(appearance).fontSize
  // #endif
  // #ifndef H5
  uni.setBackgroundColor({
    backgroundColor: dark ? '#161714' : '#E9E5DD',
    backgroundColorTop: dark ? '#161714' : '#E9E5DD',
    backgroundColorBottom: dark ? '#161714' : '#E9E5DD'
  })
  uni.setNavigationBarColor({
    frontColor: dark ? '#ffffff' : '#000000',
    backgroundColor: dark ? '#161714' : '#F8F6F2'
  })
  // #endif
}

export function saveAppearance(appearance) {
  uni.setStorageSync('ggg_appearance', appearance)
  applyAppearance(appearance)
  uni.$emit('appearanceChange', appearance)
}

export function loadSavedAppearance() {
  return uni.getStorageSync('ggg_appearance') || { theme: 'system', fontSize: 'medium' }
}
