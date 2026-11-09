/**
 * 业务 API
 */
const { request } = require('./request')

function getDepartments(params = {}) {
  return request({ url: '/api/departments', data: params })
}

function getDoctors(params = {}) {
  return request({ url: '/api/doctors', data: params })
}

function getDoctorById(id) {
  return request({ url: `/api/doctors/${id}` })
}

function getSchedules(params = {}) {
  return request({ url: '/api/schedules', data: params })
}

function getAppointments(params = {}) {
  const query = { ...params }
  if (!query.status || query.status === 'all') {
    delete query.status
  }
  return request({ url: '/api/appointments', data: query })
}

function getAppointmentById(id) {
  return request({ url: `/api/appointments/${id}` })
}

function createAppointment(data) {
  return request({ url: '/api/appointments', method: 'POST', data })
}

function payAppointment(id, payMethod) {
  return request({ url: `/api/appointments/${id}/pay`, method: 'POST', data: { payMethod } })
}

function cancelAppointment(id) {
  return request({ url: `/api/appointments/${id}/cancel`, method: 'PUT' })
}

function triageAnalyze(symptom) {
  return request({ url: '/api/triage/analyze', method: 'POST', data: { symptom }, skipAuth: true })
}

function checkInAppointment(id) {
  return request({ url: `/api/appointments/${id}/check-in`, method: 'PUT' })
}

function getConsultation(id) {
  return request({ url: `/api/appointments/${id}/consultation` })
}

function getPrescriptionByAppointment(id) {
  return request({ url: `/api/appointments/${id}/prescription` })
}

function getPrescription(id) {
  return request({ url: `/api/prescriptions/${id}` })
}

function payPrescription(id, payMethod) {
  return request({ url: `/api/prescriptions/${id}/pay`, method: 'POST', data: { payMethod } })
}

function submitAppointmentReview(id, data) {
  return request({ url: `/api/appointments/${id}/review`, method: 'POST', data })
}

function getNotifications(params = {}) {
  return request({ url: '/api/notifications', data: params })
}

function getUnreadNotificationCount() {
  return request({ url: '/api/notifications/unread-count' })
}

function markNotificationRead(id) {
  return request({ url: `/api/notifications/${id}/read`, method: 'PUT' })
}

function markAllNotificationsRead() {
  return request({ url: '/api/notifications/read-all', method: 'PUT' })
}

module.exports = {
  getDepartments,
  getDoctors,
  getDoctorById,
  getSchedules,
  getAppointments,
  getAppointmentById,
  createAppointment,
  payAppointment,
  cancelAppointment,
  submitAppointmentReview,
  triageAnalyze,
  checkInAppointment,
  getConsultation,
  getPrescriptionByAppointment,
  getPrescription,
  payPrescription,
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead
}
