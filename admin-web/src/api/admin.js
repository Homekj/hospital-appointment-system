import request from '@/utils/request'

/**
 * 管理后台接口清单，全部对应后端真实路由（见 docs/api.md 第 12 节）。
 *
 * 新版契约接口统一挂在 /api/admin/** 下，request 的 baseURL 已经是 /api。
 * 删除能力后端只保留在旧 /admin/** 上，这里用 baseURL: ROOT 单独指向后端根路径，
 * 开发时由 vite.config.js 的 /admin 代理转发。
 */
const ROOT = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/api\/?$/, '')
const legacy = { baseURL: ROOT }

export const authApi = {
  login: data => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  logout: () => request.post('/auth/logout')
}

export const dashboardApi = {
  stats: () => request.get('/admin/dashboard/stats'),
  menus: () => request.get('/admin/menus')
}

/** 公开查询接口：科室 / 医生下拉 */
export const catalogApi = {
  departments: () => request.get('/departments'),
  doctors: params => request.get('/doctors', { params }),
  schedules: params => request.get('/schedules', { params })
}

export const departmentApi = {
  page: params => request.get('/admin/departments', { params }),
  detail: id => request.get(`/admin/departments/${id}`),
  create: data => request.post('/admin/departments', data),
  update: (id, data) => request.put(`/admin/departments/${id}`, data),
  status: (id, status) => request.put(`/admin/departments/${id}/status`, { status }),
  remove: id => request.delete('/admin/department/delete', { ...legacy, params: { id } })
}

export const doctorApi = {
  page: params => request.get('/admin/doctors', { params }),
  detail: id => request.get(`/admin/doctors/${id}`),
  create: data => request.post('/admin/doctors', data),
  update: (id, data) => request.put(`/admin/doctors/${id}`, data),
  status: (id, status) => request.put(`/admin/doctors/${id}/status`, { status }),
  remove: id => request.delete('/admin/doctor/delete', { ...legacy, params: { id } })
}

export const scheduleApi = {
  page: params => request.get('/admin/schedules', { params }),
  detail: id => request.get(`/admin/schedules/${id}`),
  create: data => request.post('/admin/schedules', data),
  update: (id, data) => request.put(`/admin/schedules/${id}`, data),
  status: (id, status) => request.put(`/admin/schedules/${id}/status`, { status }),
  batch: data => request.post('/admin/schedules/batch', data),
  remove: id => request.delete(`/admin/schedule/delete/${id}`, legacy)
}

export const appointmentApi = {
  page: params => request.get('/admin/appointments', { params }),
  detail: id => request.get(`/admin/appointments/${id}`),
  statusOptions: () => request.get('/admin/appointment/status-options', legacy),
  cancel: id => request.put(`/admin/appointments/${id}/cancel`),
  checkIn: id => request.put(`/admin/appointment/${id}/check-in`, null, legacy),
  status: (id, status) => request.put(`/admin/appointment/${id}/status`, { status }, legacy),
  exportCsv: params => request.get('/admin/appointment/export', {
    ...legacy,
    params: {
      keyword: params.patientName,
      status: params.status,
      departmentId: params.departmentId,
      scheduleDateStart: params.startDate,
      scheduleDateEnd: params.endDate
    },
    responseType: 'blob'
  })
}

export const medicineApi = {
  page: params => request.get('/admin/medicines', { params }),
  detail: id => request.get(`/admin/medicines/${id}`),
  create: data => request.post('/admin/medicines', data),
  update: (id, data) => request.put(`/admin/medicines/${id}`, data),
  status: (id, status) => request.put(`/admin/medicines/${id}/status`, { status }),
  remove: id => request.delete(`/admin/medicines/${id}`)
}

export const userApi = {
  page: params => request.get('/admin/users', { params }),
  detail: id => request.get(`/admin/users/${id}`),
  status: (id, status) => request.put(`/admin/users/${id}/status`, { status })
}

export const logApi = {
  login: params => request.get('/admin/logs/login', { params }),
  operation: params => request.get('/admin/logs/operation', { params })
}
