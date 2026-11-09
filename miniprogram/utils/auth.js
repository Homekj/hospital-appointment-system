const { request } = require('./request')
const { baseUrl } = require('./config')

const TOKEN_KEY = 'token'
const USER_KEY = 'user'
const MANUAL_LOGOUT_KEY = 'auth_manual_logout'

function getToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

function getUser() {
  try {
    return wx.getStorageSync(USER_KEY) || null
  } catch (e) {
    return null
  }
}

function setAuth(token, user) {
  wx.setStorageSync(TOKEN_KEY, token)
  wx.setStorageSync(USER_KEY, user)
  wx.removeStorageSync(MANUAL_LOGOUT_KEY)
  const app = getApp()
  if (app) {
    app.globalData.token = token
    app.globalData.user = user
  }
}

function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
  const app = getApp()
  if (app) {
    app.globalData.token = ''
    app.globalData.user = null
  }
}

function isLoggedIn() {
  return !!getToken()
}

function shouldAutoLogin() {
  try {
    return !wx.getStorageSync(MANUAL_LOGOUT_KEY)
  } catch (e) {
    return true
  }
}

function passwordLogin(username, password) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    skipAuth: true,
    data: { username, password }
  }).then((data) => {
    setAuth(data.token, data.user)
    return data
  })
}

function wxLogin(options = {}) {
  return new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          reject(new Error('获取微信 code 失败'))
          return
        }
        request({
          url: '/api/auth/wx-login',
          method: 'POST',
          skipAuth: true,
          data: {
            code: loginRes.code,
            nickname: options.nickname || ''
          }
        }).then((data) => {
          setAuth(data.token, data.user)
          const avatarPath = options.avatarPath
          const nickname = (options.nickname || '').trim()
          if (avatarPath || nickname) {
            syncWxProfile({ nickname, avatarPath })
              .then((user) => {
                resolve({ token: getToken(), user })
              })
              .catch(reject)
            return
          }
          resolve(data)
        }).catch(reject)
      },
      fail(err) {
        reject(new Error(err.errMsg || 'wx.login 失败'))
      }
    })
  })
}

function uploadAvatarFile(filePath) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    if (!token) {
      reject(new Error('请先登录'))
      return
    }
    wx.uploadFile({
      url: `${baseUrl}/api/files/upload`,
      filePath,
      name: 'file',
      formData: { category: 'avatar' },
      header: { Authorization: `Bearer ${token}` },
      success(res) {
        let body
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          reject(new Error('上传响应解析失败'))
          return
        }
        if (res.statusCode === 401) {
          sessionExpired()
          reject(new Error((body && body.message) || '登录已过期'))
          return
        }
        if (!body || body.code !== 200) {
          reject(new Error((body && body.message) || '头像上传失败'))
          return
        }
        resolve(body.data)
      },
      fail(err) {
        reject(new Error(err.errMsg || '头像上传失败'))
      }
    })
  })
}

function bindAvatar(path) {
  return request({
    url: '/api/profile/avatar',
    method: 'PUT',
    data: { path }
  })
}

function updateProfile(data) {
  const current = getUser() || {}
  return request({
    url: '/api/profile',
    method: 'PUT',
    data: {
      nickname: data.nickname !== undefined ? data.nickname : (current.nickname || ''),
      email: data.email !== undefined ? data.email : (current.email || ''),
      phone: data.phone !== undefined ? data.phone : (current.phone || '')
    }
  }).then((user) => {
    setAuth(getToken(), user)
    return user
  })
}

function syncWxProfile({ nickname, avatarPath } = {}) {
  let chain = Promise.resolve(getUser())
  const trimmedNickname = (nickname || '').trim()
  if (trimmedNickname) {
    chain = chain.then(() => updateProfile({ nickname: trimmedNickname }))
  }
  if (avatarPath) {
    chain = chain
      .then(() => uploadAvatarFile(avatarPath))
      .then((fileVo) => bindAvatar(fileVo.path))
      .then((user) => {
        setAuth(getToken(), user)
        return user
      })
  }
  return chain
}

function fetchMe() {
  return request({ url: '/api/auth/me' }).then((user) => {
    const token = getToken()
    if (token) {
      setAuth(token, user)
    }
    return user
  })
}

function logout() {
  clearAuth()
  wx.setStorageSync(MANUAL_LOGOUT_KEY, true)
}

function sessionExpired() {
  clearAuth()
}

module.exports = {
  getToken,
  getUser,
  isLoggedIn,
  shouldAutoLogin,
  passwordLogin,
  wxLogin,
  fetchMe,
  updateProfile,
  syncWxProfile,
  logout,
  sessionExpired
}
