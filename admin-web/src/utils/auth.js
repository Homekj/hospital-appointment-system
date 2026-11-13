const TOKEN_KEY = 'hospital_admin_token'
const USER_KEY = 'hospital_admin_user'

export const ROLE_HOME = {
  ADMIN: '/admin/dashboard',
  DOCTOR: '/doctor/dashboard',
  USER: '/patient/home'
}

export const ROLE_LABEL = {
  ADMIN: '管理员',
  DOCTOR: '医生',
  USER: '患者'
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch (e) {
    return {}
  }
}

export function getRole() {
  return (getUser().role || '').toUpperCase()
}

export function roleHome(role) {
  return ROLE_HOME[(role || '').toUpperCase()] || '/login'
}

export function setAuth(loginData) {
  localStorage.setItem(TOKEN_KEY, loginData.token || '')
  localStorage.setItem(USER_KEY, JSON.stringify(loginData.user || {}))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
