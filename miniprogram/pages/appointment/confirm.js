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
    profileChoices: [],
    profileIndex: 0,
    patientProfileId: null,
    patientName: '',
    patientPhone: '',
    remark: '',
    feeText: '',
    feeAmount: '',
    scheduleError: '',
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
    if (options.fromAppointment && /^[1-9][0-9]*$/.test(options.fromAppointment)) {
      this.profileChosen = true
      api.getAppointmentById(options.fromAppointment).then(source => {
        if (source.cancellationType === 'schedule_stopped' && !this.data.patientProfileId
            && !this.data.patientName && !this.data.patientPhone) {
          this.setData({ patientName: source.patientName || '', patientPhone: source.patientPhone || '' })
        }
      }).catch(() => toast('无法加载原就诊人信息，请重新填写'))
    }
    this.slot = {
      doctorId: Number(options.doctorId),
      scheduleDate: options.scheduleDate || '',
      scheduleId: Number(options.scheduleId)
    }
    api.getDoctorById(this.slot.doctorId)
      .then(doctor => this.setData({ doctor: enrichDoctor(doctor, true), deptName: doctor.departmentName || '' }))
      .catch(err => toast(err.message || '加载医生失败'))
    this.loadSchedule()
    this.loadBalance()
  },

  async loadSchedule() {
    const { doctorId, scheduleDate, scheduleId } = this.slot
    this.setData({ scheduleId: 0, scheduleError: '' })
    try {
      const schedules = await api.getSchedules({ doctorId, scheduleDate })
      const selected = schedules.find(slot => slot.id === scheduleId && Number(slot.remainQuota) > 0)
      if (!selected || selected.registrationFee == null) {
        this.setData({ scheduleError: '当前号源已不可预约，请返回重新选择。' })
        return
      }
      const feeAmount = Number(selected.registrationFee).toFixed(2)
      const timeSlot = selected.timeSlot
      this.setData({
        scheduleDate, timeSlot, scheduleId,
        feeText: formatFee(feeAmount),
        feeAmount,
        timeText: formatDateTime(scheduleDate, timeSlot)
      })
    } catch (error) {
      this.setData({ scheduleError: error.message || '加载号源失败' })
    }
  },

  async onShow() {
    if (!auth.isLoggedIn()) return
    try {
      const profiles = await api.getPatients()
      const profileChoices = [{ id: null, label: '手动填写' }].concat(profiles.map(p => ({
        ...p,
        label: p.name + ' · ' + p.relationship + (p.defaultPatient ? '（默认）' : '')
      })))
      const selected = this.profileChosen
        ? profileChoices.find(p => p.id === this.data.patientProfileId)
        : profileChoices.find(p => p.defaultPatient)
      const profile = selected || profileChoices[0]
      this.setData({ profileChoices, profileIndex: profileChoices.indexOf(profile), patientProfileId: profile.id })
    } catch (error) {
      toast(error.message || '常用就诊人加载失败，可手动填写')
    }
  },

  onProfileChange(e) {
    const profileIndex = Number(e.detail.value)
    this.profileChosen = true
    this.setData({ profileIndex, patientProfileId: this.data.profileChoices[profileIndex].id })
  },

  managePatients() {
    wx.navigateTo({ url: '/pages/mine/patients/patients' })
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
    this.profileChosen = true
    this.setData({ patientName: e.detail })
  },

  onPatientPhoneInput(e) {
    this.profileChosen = true
    this.setData({ patientPhone: e.detail })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail })
  },

  onPayMethodTap(e) {
    const { id } = e.currentTarget.dataset
    if (!id) return
    this.setData({ payMethod: id })
  },

  onPayDrawerClose() {
    this.setData({ payDrawerVisible: false })
  },

  onPayDrawerConfirm() {
    this.doSubmit()
  },

  onConfirm() {
    if (this.data.submitting || !this.data.scheduleId) return
    const { payMethod } = this.data
    if (needsPayDrawer(payMethod)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: payMethod })
      return
    }
    this.doSubmit()
  },

  async doSubmit() {
    if (this.data.submitting) return
    const { scheduleId, patientProfileId, patientName, patientPhone, remark, payMethod, feeAmount, balance } = this.data
    if (!scheduleId) { toast('请重新选择预约时段'); return }
    if (!patientProfileId && (!patientName.trim() || !/^1[3-9]\d{9}$/.test(patientPhone))) {
      toast('请填写姓名和有效手机号')
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
    let appt
    try {
      const patient = patientProfileId ? { patientProfileId } : { patientName: patientName.trim(), patientPhone }
      appt = await api.createAppointment({ scheduleId, ...patient, remark, expectedRegistrationFee: Number(feeAmount) })
      await mockChannelPay(payMethod)
      await api.payAppointment(appt.id, payMethod)
      auth.fetchMe().catch(() => {})
      this.setData({ payDrawerVisible: false })
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.openOrder(appt.id)
    } catch (err) {
      if (appt) {
        this.setData({ payDrawerVisible: false })
        wx.showModal({
          title: '请查看预约订单',
          content: (err.message || '支付未完成') + '。订单已创建，可在订单详情中查看状态或继续支付。',
          showCancel: false,
          success: () => this.openOrder(appt.id)
        })
      } else {
        this.setData({ payDrawerVisible: false })
        toast(err.message || '预约失败，可在订单页查看是否已生成预约')
        if (err.message && err.message.includes('挂号费已调整')) this.loadSchedule()
      }
    } finally {
      this.setData({ submitting: false })
    }
  },

  openOrder(id) {
    wx.redirectTo({ url: '/pages/order/detail?id=' + id })
  }
})
