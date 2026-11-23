const api = require('../../utils/api')
const { enrichDoctor, groupSchedules } = require('../../utils/adapter')
const { buildDateStrip, formatFee } = require('../../utils/format')
const toast = require('../../utils/toast')

Page({
  data: {
    doctor: null,
    dateList: [],
    selectedDate: '',
    morning: [],
    afternoon: [],
    hasAnySchedule: false,
    hasAvailable: false,
    selectedTime: '',
    selectedScheduleId: 0,
    feeText: ''
  },

  onLoad(options) {
    this.fromAppointment = options.fromAppointment || ''
    const doctorId = Number(options.doctorId)
    if (!doctorId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    const dateList = buildDateStrip(7)
    const selectedDate = dateList[0].value
    this.setData({ dateList, selectedDate, doctorId })
    api.getDoctorById(doctorId)
      .then((doctor) => {
        this.setData({
          doctor: enrichDoctor(doctor, true)
        })
        this.loadSlots()
      })
      .catch((err) => toast(err.message || '医生不存在'))
  },

  loadSlots() {
    const { doctorId, selectedDate } = this.data
    this.schedules = []
    this.setData({ selectedTime: '', selectedScheduleId: 0, feeText: '', morning: [], afternoon: [], hasAvailable: false })
    api.getSchedules({ doctorId, scheduleDate: selectedDate })
      .then((schedules) => {
        if (selectedDate !== this.data.selectedDate) return
        this.schedules = schedules
        const slots = groupSchedules(schedules)
        this.setData({
          morning: slots.morning,
          afternoon: slots.afternoon,
          hasAnySchedule: slots.hasAnySchedule,
          hasAvailable: slots.hasAvailable
        })
      })
      .catch((err) => toast(err.message || '加载号源失败'))
  },

  onDateChange(e) {
    this.setData({ selectedDate: e.detail.value })
    this.loadSlots()
  },

  onSlotSelect(e) {
    const selected = (this.schedules || []).find(slot => slot.id === e.detail.scheduleId)
    if (!selected) return
    this.setData({
      selectedTime: selected.timeSlot,
      selectedScheduleId: selected.id,
      feeText: formatFee(selected.registrationFee)
    })
  },

  onBook() {
    const { doctor, selectedDate, selectedTime, selectedScheduleId } = this.data
    if (!selectedTime || !selectedScheduleId) {
      wx.showToast({ title: '请选择就诊时段', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/appointment/confirm?doctorId=${doctor.id}&scheduleDate=${selectedDate}&timeSlot=${selectedTime}&scheduleId=${selectedScheduleId}${this.fromAppointment ? '&fromAppointment=' + encodeURIComponent(this.fromAppointment) : ''}`
    })
  }
})
