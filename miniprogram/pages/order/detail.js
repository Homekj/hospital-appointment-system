const api = require('../../utils/api')
const auth = require('../../utils/auth')
const { toOrder } = require('../../utils/adapter')
const { formatDateTime, formatFee } = require('../../utils/format')
const { PAY_METHODS, mockChannelPay, needsPayDrawer } = require('../../utils/pay')
const toast = require('../../utils/toast')
const { goDeptTab } = require('../../utils/tab')

const REFUND_TEXT = {
  not_required: '未支付，无需退款',
  balance_refunded: '已退回账户余额',
  mock_refunded: '已登记模拟退款',
  contact_hospital: '请联系医院确认'
}

Page({
  data: {
    order: null,
    detailItems: [],
    feeText: '',
    rxFeeText: '',
    balance: '0.00',
    payMethods: PAY_METHODS,
    payMethod: 'wechat',
    payDrawerVisible: false,
    payDrawerMethod: 'wechat',
    paying: false
  },

  onLoad(options) {
    this.orderId = options.id
  },

  onShow() {
    if (this.orderId) this.loadOrder(this.orderId)
    this.loadBalance()
    this.queueTimer = setInterval(() => {
      const order = this.data.order
      if (order && ['unpaid', 'checked_in'].includes(order.rawStatus)) this.loadOrder(this.orderId, true)
    }, 15000)
  },

  onHide() { clearInterval(this.queueTimer) },
  onUnload() { clearInterval(this.queueTimer) },

  refreshOrder() { this.loadOrder(this.orderId) },

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

  loadOrder(id, silent = false) {
    const requestId = this.orderRequest = (this.orderRequest || 0) + 1
    api.getAppointmentById(id)
      .then((appt) => {
        if (this.orderRequest !== requestId) return
        const order = toOrder(appt)
        const items = [
          { label: '就诊医生', value: order.doctorName },
          { label: '就诊科室', value: order.departmentName },
          { label: '就诊时间', value: formatDateTime(order.scheduleDate, order.timeSlot) },
          { label: '就诊人', value: order.patientName },
          { label: '联系电话', value: order.patientPhone },
          { label: '挂号费用', value: formatFee(order.fee) },
          { label: '订单编号', value: order.orderNo }
        ]
        if (order.payMethodText) {
          items.push({ label: '支付方式', value: order.payMethodText })
        }
        if (order.prescriptionAmount > 0) {
          items.push({ label: '处方费用', value: formatFee(order.prescriptionAmount) })
        }
        this.setData({
          order,
          payDrawerVisible: order.canPay && this.data.payDrawerVisible,
          detailItems: items,
          feeText: formatFee(order.fee),
          rxFeeText: formatFee(order.prescriptionAmount),
          refundText: REFUND_TEXT[order.refundStatus] || ''
        })
      })
      .catch((err) => { if (!silent) toast(err.message || '无法刷新预约详情') })
  },

  onPayMethodTap(e) {
    const { id } = e.currentTarget.dataset
    if (!id) return
    this.setData({ payMethod: id })
    if (needsPayDrawer(id)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: id })
    }
  },

  onPayDrawerClose() {
    this.setData({ payDrawerVisible: false })
  },

  onPayDrawerConfirm() {
    this.doPay()
  },

  onPay() {
    if (this.data.paying || !this.data.order || !this.data.order.canPay) return
    const { payMethod } = this.data
    if (needsPayDrawer(payMethod)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: payMethod })
      return
    }
    this.doPay()
  },

  async doPay() {
    if (this.data.paying || !this.data.order || !this.data.order.canPay) return
    const { payMethod, order, balance } = this.data
    if (payMethod === 'balance' && Number(order.fee) > Number(balance)) {
      toast('余额不足，请先充值')
      return
    }
    this.setData({ paying: true })
    try {
      await mockChannelPay(payMethod)
      await api.payAppointment(order.id, payMethod)
      await auth.fetchMe()
      this.setData({ payDrawerVisible: false })
      wx.showToast({ title: '支付成功', icon: 'success' })
      this.loadOrder(order.id)
      this.loadBalance()
    } catch (err) {
      toast(err.message || '支付失败')
      this.loadOrder(order.id, true)
    } finally {
      this.setData({ paying: false })
    }
  },

  onCancel() {
    wx.showModal({
      title: '取消预约',
      content: '确定要取消该预约吗？',
      success: (res) => {
        if (res.confirm) {
          api.cancelAppointment(this.data.order.id)
            .then(() => {
              this.loadOrder(this.data.order.id)
              wx.showToast({ title: '已取消', icon: 'success' })
            })
            .catch((err) => toast(err.message || '取消失败'))
        }
      }
    })
  },

  onEvaluate() {
    wx.navigateTo({ url: `/pages/order/evaluate?id=${this.data.order.id}` })
  },

  onCheckIn() {
    const { order } = this.data
    api.checkInAppointment(order.id)
      .then(() => {
        wx.showToast({ title: '签到成功', icon: 'success' })
        this.loadOrder(order.id)
      })
      .catch((err) => {
        toast(err.message || '签到失败')
        this.loadOrder(order.id, true)
      })
  },

  onRebook() {
    const { order } = this.data
    goDeptTab({ deptId: order.departmentId, keyword: '', fromAppointment: order.id })
  },

  onViewRecord() {
    wx.navigateTo({ url: `/pages/visit/record?id=${this.data.order.id}` })
  },

  onViewPrescription() {
    const { order } = this.data
    if (order.canPayPrescription) {
      wx.navigateTo({ url: `/pages/prescription/detail?id=${order.prescriptionId}&appointmentId=${order.id}` })
    } else {
      wx.navigateTo({ url: `/pages/prescription/detail?appointmentId=${order.id}` })
    }
  },

  onPrescription() {
    this.onViewPrescription()
  },

  onRecharge() {
    wx.navigateTo({ url: '/pages/recharge/recharge' })
  }
})
