const { enrichDepartment, enrichDepartments } = require('./dept-meta')
const { resolveMediaUrl } = require('./media')

function enrichDoctor(doctor, hasSlot) {
  const avatar = doctor.avatar || ''
  const intro = (doctor.intro || '').replace(/[。.]+$/, '')
  const hasRating = doctor.rating != null && (doctor.reviewCount || 0) > 0
  return {
    ...doctor,
    intro,
    hospital: '市第一人民医院',
    rating: hasRating ? Number(doctor.rating).toFixed(1) : '暂无',
    consultCount: doctor.consultCount || 0,
    fee: 20,
    hasSlot: hasSlot !== false,
    avatar,
    avatarUrl: resolveMediaUrl(avatar)
  }
}

const MORNING_SLOTS = ['08:00', '09:00', '10:00']
const AFTERNOON_SLOTS = ['14:00', '15:00', '16:00']

function normalizeSlotTime(timeSlot) {
  const t = String(timeSlot || '').trim()
  if (t === '上午') return '09:00'
  if (t === '下午') return '14:00'
  return t
}

function toSlotItem(schedule) {
  const time = normalizeSlotTime(schedule.timeSlot)
  const remain = schedule.remainQuota != null
    ? schedule.remainQuota
    : (schedule.totalQuota - schedule.bookedCount)
  const active = schedule.status !== 0 && schedule.status !== '0'
  return {
    time,
    available: active && remain > 0,
    scheduleId: schedule.id,
    remainQuota: Math.max(remain, 0),
    full: active && remain <= 0,
    noSchedule: false
  }
}

function buildSlotGrid(schedules) {
  const slotMap = {}
  const extras = { morning: [], afternoon: [] }

  ;(schedules || []).forEach((s) => {
    const item = toSlotItem(s)
    const hour = parseInt(item.time.split(':')[0], 10) || 0
    const inTemplate = MORNING_SLOTS.includes(item.time) || AFTERNOON_SLOTS.includes(item.time)
    if (inTemplate) {
      slotMap[item.time] = item
    } else if (hour < 12) {
      extras.morning.push(item)
    } else {
      extras.afternoon.push(item)
    }
  })

  const fill = (template) => template.map((time) => slotMap[time] || {
    time,
    available: false,
    scheduleId: 0,
    remainQuota: 0,
    full: false,
    noSchedule: true
  })

  const morning = [...fill(MORNING_SLOTS), ...extras.morning]
  const afternoon = [...fill(AFTERNOON_SLOTS), ...extras.afternoon]
  const all = morning.concat(afternoon)

  return {
    morning,
    afternoon,
    hasAnySchedule: (schedules || []).length > 0,
    hasAvailable: all.some((s) => s.available)
  }
}

function groupSchedules(schedules) {
  return buildSlotGrid(schedules)
}

function mapAppointmentStatus(status) {
  if (status === 'confirmed') return 'pending'
  return status
}

function resolveDisplayStatus(appt) {
  const rawStatus = appt.status
  if (rawStatus === 'pending_rx_pay' && appt.prescriptionStatus === 'paid') {
    return 'pending_dispense'
  }
  return mapAppointmentStatus(rawStatus)
}

function toOrder(appt) {
  const fee = appt.amount != null ? appt.amount : 20
  const rawStatus = appt.status
  const displayStatus = resolveDisplayStatus(appt)
  return {
    id: String(appt.id),
    orderNo: `AP${String(appt.id).padStart(8, '0')}`,
    doctorId: appt.doctorId || 0,
    doctorName: appt.doctorName || '',
    doctorTitle: appt.doctorTitle || '',
    departmentId: appt.departmentId || 0,
    departmentName: appt.departmentName || '',
    scheduleDate: appt.scheduleDate || '',
    timeSlot: appt.timeSlot || '',
    patientName: appt.patientName || '',
    patientPhone: appt.patientPhone || '',
    fee,
    amount: fee,
    payMethod: appt.payMethod || '',
    payMethodText: appt.payMethodText || '',
    payTime: appt.payTime || '',
    paymentDeadline: appt.paymentDeadline || '',
    paymentExpired: !!appt.paymentExpired,
    cancellationType: appt.cancellationType || '',
    cancellationReason: appt.cancellationReason || '',
    refundStatus: appt.refundStatus || '',
    queueDeferredAt: appt.queueDeferredAt || '',
    status: displayStatus,
    rawStatus,
    displayStatus,
    prescriptionId: appt.prescriptionId || null,
    prescriptionStatus: appt.prescriptionStatus || '',
    prescriptionAmount: appt.prescriptionAmount != null ? appt.prescriptionAmount : 0,
    hasConsultation: !!appt.hasConsultation,
    canPay: rawStatus === 'unpaid' && !appt.paymentExpired,
    canCheckIn: rawStatus === 'pending' || rawStatus === 'confirmed',
    canCancel: ['unpaid', 'pending', 'confirmed'].includes(rawStatus),
    canPayPrescription: rawStatus === 'pending_rx_pay' && appt.prescriptionStatus === 'unpaid',
    canViewRecord: !!appt.hasConsultation,
    canViewPrescription: !!appt.prescriptionId,
    location: appt.departmentLocation || '',
    checkInTime: appt.checkInTime || '',
    waitingAhead: appt.waitingAhead,
    inConsultationCount: appt.inConsultationCount,
    remark: appt.remark || '',
    createTime: appt.createTime || '',
    evaluated: !!appt.evaluated
  }
}

module.exports = {
  enrichDepartment,
  enrichDepartments,
  enrichDoctor,
  buildSlotGrid,
  groupSchedules,
  toOrder,
  resolveDisplayStatus
}
