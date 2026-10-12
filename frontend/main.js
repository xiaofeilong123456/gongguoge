import App from './App'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
import ThemePage from './components/ThemePage.vue'
import { applyAppearance, loadSavedAppearance } from './utils/appearance'
Vue.config.productionTip = false
Vue.component('ThemePage', ThemePage)
applyAppearance(loadSavedAppearance())
App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif
let __uniapp_ready__ = true
