const auth = require('../../utils/auth')
const toast = require('../../utils/toast')
const api = require('../../utils/api')
const { enrichDoctor } = require('../../utils/adapter')
const { formatDateTime, formatFee } = require('../../utils/format')
const { PAY_METHODS, mockChannelPay, needsPayDrawer } = require('../../utils/pay')

Page({
  data: {
    doctor: null,
    deptName: '',
    scheduleDate: '',
    timeSlot: '',
    scheduleId: 0,
    patientName: '',
    patientPhone: '',
    remark: '',
    feeText: '',
    feeAmount: '20.00',
    timeText: '',
    balance: '0.00',
    payMethods: PAY_METHODS,
    payMethod: 'wechat',
    payDrawerVisible: false,
    payDrawerMethod: 'wechat',
    submitting: false
  },

  onLoad(options) {
    if (!auth.isLoggedIn()) {
      wx.showModal({
        title: '提示',
        content: '预约需要先登录，是否前往登录？',
        success: (res) => {
          if (res.confirm) wx.switchTab({ url: '/pages/mine/mine' })
          else wx.navigateBack()
        }
      })
    }
    const doctorId = Number(options.doctorId)
    const scheduleDate = options.scheduleDate || ''
    const timeSlot = options.timeSlot || ''
    const scheduleId = Number(options.scheduleId) || 0
    api.getDoctorById(doctorId)
      .then((doctor) => {
        const enriched = enrichDoctor(doctor, true)
        const feeAmount = Number(enriched.fee || 20).toFixed(2)
        this.setData({
          doctor: enriched,
          deptName: doctor.departmentName || '',
          scheduleDate,
          timeSlot,
          scheduleId,
          feeText: formatFee(feeAmount),
          feeAmount,
          timeText: formatDateTime(scheduleDate, timeSlot)
        })
      })
      .catch((err) => toast(err.message || '加载失败'))
    this.loadBalance()
  },

  async loadBalance() {
    if (!auth.isLoggedIn()) return
    try {
      const user = await auth.fetchMe()
      const balance = user.balance != null ? Number(user.balance).toFixed(2) : '0.00'
      const payMethods = PAY_METHODS.map((item) => ({
        ...item,
        desc: item.id === 'balance' ? `可用 ¥${balance}` : item.desc
      }))
      this.setData({ balance, payMethods })
    } catch (e) {
      // ignore
    }
  },

  onPatientNameInput(e) {
    this.setData({ patientName: e.detail })
  },

  onPatientPhoneInput(e) {
    this.setData({ patientPhone: e.detail })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail })
  },

  onPayMethodTap(e) {
    const { id } = e.currentTarget.dataset
    if (!id) return
    this.setData({ payMethod: id })
    if (needsPayDrawer(id)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: id })
    } else {
      this.setData({ payDrawerVisible: false })
    }
  },

  onPayDrawerClose() {
    this.setData({ payDrawerVisible: false })
  },

  onPayDrawerConfirm() {
    this.doSubmit()
  },

  onConfirm() {
    if (this.data.submitting) return
    const { payMethod } = this.data
    if (needsPayDrawer(payMethod)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: payMethod })
      return
    }
    this.doSubmit()
  },

  async doSubmit() {
    if (this.data.submitting) return
    const { scheduleId, patientName, patientPhone, remark, payMethod, feeAmount, balance } = this.data
    if (!patientName || !patientPhone) {
      toast('请填写就诊人信息')
      return
    }
    if (!auth.isLoggedIn()) {
      toast('请先登录')
      wx.switchTab({ url: '/pages/mine/mine' })
      return
    }
    if (payMethod === 'balance' && Number(feeAmount) > Number(balance)) {
      toast('余额不足，请先充值')
      return
    }
    this.setData({ submitting: true })
    try {
      await mockChannelPay(payMethod)
      const appt = await api.createAppointment({
        scheduleId,
        patientName,
        patientPhone,
        remark
      })
      await api.payAppointment(appt.id, payMethod)
      await auth.fetchMe()
      this.setData({ payDrawerVisible: false })
      wx.showToast({ title: '支付成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order/detail?id=${appt.id}` })
      }, 800)
    } catch (err) {
      toast(err.message || '预约失败')
    } finally {
      this.setData({ submitting: false })
    }
  }
})
