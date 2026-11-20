import request from '@/utils/request'

/**
 * 医生工作台接口（/api/doctor/**）。
 * 身份全部由 JWT 中的 doctorId 决定，不需要也不允许传 doctorId 作为数据主体。
 */
export const doctorApi = {
  me: () => request.get('/doctor/me'),
  updatePassword: data => request.put('/doctor/password', data),
  updateAvatar: path => request.put('/doctor/avatar', { path }),
  stats: () => request.get('/doctor/stats'),
  dashboard: () => request.get('/doctor/dashboard'),
  schedules: params => request.get('/doctor/schedules', { params }),
  appointments: params => request.get('/doctor/appointments', { params }),
  detail: id => request.get(`/doctor/appointments/${id}`),
  clinical: id => request.get(`/doctor/appointments/${id}/clinical`),
  saveClinical: (id, data) => request.put(`/doctor/appointments/${id}/clinical`, data),
  start: id => request.post(`/doctor/appointments/${id}/start`),
  complete: (id, prescriptionItems) => request.post(
    `/doctor/appointments/${id}/complete`, { prescriptionItems }
  ),
  medicines: keyword => request.get('/doctor/medicines', { params: { keyword } })
}
