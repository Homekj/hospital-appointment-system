/**
 * 后端部分 VO 用 LocalDateTime + @JsonFormat，Jackson 版本不同可能输出
 * "yyyy-MM-dd HH:mm:ss"、"yyyy-MM-ddTHH:mm:ss" 或 [年,月,日,时,分,秒] 数组，
 * 这里做统一容错展示。
 */
function pad(n) {
  return String(n).padStart(2, '0')
}

export function formatDateTime(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

export function formatDate(value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    return `${value[0]}-${pad(value[1])}-${pad(value[2])}`
  }
  return String(value).slice(0, 10)
}

/** 预约状态：严格按 sql/system.sql 的七个值，中文标签来自后端 AppointmentServiceImpl */
export const APPOINTMENT_STATUS = {
  unpaid: { label: '待支付', type: 'warning' },
  pending: { label: '待就诊', type: 'primary' },
  checked_in: { label: '已到院', type: 'success' },
  in_consultation: { label: '就诊中', type: 'success' },
  pending_rx_pay: { label: '待缴药费', type: 'warning' },
  completed: { label: '已完成', type: 'info' },
  cancelled: { label: '已取消', type: 'danger' },
  pending_dispense: { label: '待取药', type: 'warning' }
}

export const STATUS_OPTIONS = Object.keys(APPOINTMENT_STATUS)
  .filter(key => key !== 'pending_dispense')
  .map(value => ({ value, label: APPOINTMENT_STATUS[value].label }))

export function statusText(value) {
  if (value === 'confirmed') value = 'pending'
  return (APPOINTMENT_STATUS[value] && APPOINTMENT_STATUS[value].label) || value || '-'
}

export function statusTagType(value) {
  return (APPOINTMENT_STATUS[value] && APPOINTMENT_STATUS[value].type) || 'info'
}

export const PAY_METHODS = [
  { value: 'wechat', label: '微信支付（模拟）' },
  { value: 'alipay', label: '支付宝（模拟）' },
  { value: 'balance', label: '余额支付' }
]

export function payMethodText(value) {
  const map = { wechat: '微信支付', alipay: '支付宝', balance: '余额支付' }
  return map[value] || value || '-'
}
