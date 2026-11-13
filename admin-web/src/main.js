import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'
import { formatDateTime, formatDate, statusText } from './utils/format'

Vue.config.productionTip = false
Vue.use(ElementUI)

// 全局小工具：金额、状态、手机号脱敏
Vue.filter('money', value => {
  const num = Number(value || 0)
  return '¥' + num.toFixed(2)
})
Vue.filter('maskPhone', value => {
  if (!value) return '-'
  const text = String(value)
  return text.length < 7 ? text : text.slice(0, 3) + '****' + text.slice(-4)
})
Vue.filter('time', formatDateTime)
Vue.filter('date', formatDate)
Vue.filter('statusText', statusText)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
