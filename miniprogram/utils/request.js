const { baseUrl } = require('./config')

const TOKEN_KEY = 'token'

function getToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = options.skipAuth ? '' : getToken()
    wx.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {})
      },
      success(res) {
        const body = res.data
        if (res.statusCode === 401) {
          try {
            require('./auth').sessionExpired()
          } catch (e) {
            // ignore
          }
          reject(new Error((body && body.message) || '未登录或登录已过期'))
          return
        }
        if (res.statusCode >= 500 && (!body || typeof body !== 'object' || body.code == null)) {
          reject(new Error('后端未启动'))
          return
        }
        if (!body || body.code !== 200) {
          reject(new Error((body && body.message) || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail(err) {
        const msg = err.errMsg || '网络错误'
        if (msg.indexOf('url not in domain list') !== -1) {
          reject(new Error('请求地址未通过校验：请检查同网局域网或配置 HTTPS 域名'))
          return
        }
        if (msg.indexOf('connect') !== -1 || msg.indexOf('timeout') !== -1 || msg.indexOf('fail') !== -1) {
          reject(new Error('后端未启动'))
          return
        }
        reject(new Error(msg))
      }
    })
  })
}

module.exports = { request, getToken }
