import request from '@/utils/request'

/**
 * 患者中心接口（/api/appointments、/api/notifications、/api/wallet、/api/profile 等）。
 * 全部由 RoleAccessPolicy 限定为 USER 角色，身份取自 JWT。
 */
export const patientApi = {
  // 公开查询：科室 / 医生 / 号源
  departments: () => request.get('/departments'),
  doctors: params => request.get('/doctors', { params }),
  schedules: params => request.get('/schedules', { params }),
  triage: symptom => request.post('/triage/analyze', { symptom }),

  // 预约
  appointments: params => request.get('/appointments', { params }),
  appointmentDetail: id => request.get(`/appointments/${id}`),
  createAppointment: data => request.post('/appointments', data),
  payAppointment: (id, payMethod) => request.post(`/appointments/${id}/pay`, { payMethod }),
  cancelAppointment: id => request.put(`/appointments/${id}/cancel`),
  checkIn: id => request.put(`/appointments/${id}/check-in`),

  // 病历 / 评价 / 处方
  consultation: id => request.get(`/appointments/${id}/consultation`),
  review: (id, data) => request.post(`/appointments/${id}/review`, data),
  prescriptionByAppointment: id => request.get(`/appointments/${id}/prescription`),
  prescription: id => request.get(`/prescriptions/${id}`),
  payPrescription: (id, payMethod) => request.post(`/prescriptions/${id}/pay`, { payMethod }),
  pickupPrescription: id => request.put(`/prescriptions/${id}/pickup`),

  // 消息
  notifications: params => request.get('/notifications', { params }),
  unreadCount: () => request.get('/notifications/unread-count'),
  readNotification: id => request.put(`/notifications/${id}/read`),
  readAll: () => request.put('/notifications/read-all'),

  // 余额
  recharge: data => request.post('/wallet/recharge', data),
  walletLogs: params => request.get('/wallet/logs', { params }),

  // 个人资料
  updateProfile: data => request.put('/profile', data),
  updateAvatar: path => request.put('/profile/avatar', { path }),
  changePassword: data => request.put('/profile/password/change', data)
}
