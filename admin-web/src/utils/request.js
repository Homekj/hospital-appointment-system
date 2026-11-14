import axios from 'axios'
import { Message } from 'element-ui'
import { getToken, clearAuth } from './auth'

/**
 * 后端统一响应：{ code, data, message }（api.md 3.1）
 * 成功 code === 200；旧 /admin/** 删除接口失败时是 HTTP 200 + code=500，同样在这里拦截。
 * 拦截器成功时直接返回 data，页面无需再写 res.data.data。
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json; charset=UTF-8' }
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const body = response.data
    if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (body.code === 200) {
        return body.data
      }
      const message = body.message || '请求失败'
      Message.error(message)
      return Promise.reject(new Error(message))
    }
    return body
  },
  error => {
    const status = error.response && error.response.status
    const payload = error.response && error.response.data
    let message = (payload && payload.message) || error.message || '网络异常'

    if (status === 401) {
      message = '登录已过期，请重新登录'
      clearAuth()
      const redirect = encodeURIComponent(window.location.hash.replace(/^#/, '') || '/admin/dashboard')
      window.location.hash = '#/login?redirect=' + redirect
      window.location.reload()
    } else if (status === 403) {
      message = (payload && payload.message) || '无权访问此资源'
    } else if (status === 404) {
      message = (payload && payload.message) || '请求的接口不存在'
    } else if (!status) {
      message = '无法连接后端服务，请确认 Spring Boot 已在 9090 端口启动'
    }
    Message.error(message)
    return Promise.reject(error)
  }
)

export default service
