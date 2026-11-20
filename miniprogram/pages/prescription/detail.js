const api = require('../../utils/api')
const auth = require('../../utils/auth')
const { formatFee } = require('../../utils/format')
const { PAY_METHODS, mockChannelPay, needsPayDrawer } = require('../../utils/pay')
const toast = require('../../utils/toast')

Page({
  data: {
    prescription: null,
    items: [],
    amountText: '',
    canPay: false,
    canPickup: false,
    balance: '0.00',
    payMethods: PAY_METHODS,
    payMethod: 'wechat',
    payDrawerVisible: false,
    payDrawerMethod: 'wechat',
    paying: false,
    pickuping: false
  },

  onLoad(options) {
    this.appointmentId = options.appointmentId
    this.prescriptionId = options.id
    this.loadData()
    this.loadBalance()
  },

  loadBalance() {
    if (!auth.isLoggedIn()) return
    auth.fetchMe().then((user) => {
      const balance = user.balance != null ? Number(user.balance).toFixed(2) : '0.00'
      const payMethods = PAY_METHODS.map((item) => ({
        ...item,
        desc: item.id === 'balance' ? `可用 ¥${balance}` : item.desc
      }))
      this.setData({ balance, payMethods })
    }).catch(() => {})
  },

  loadData() {
    const loader = this.prescriptionId
      ? api.getPrescription(this.prescriptionId)
      : api.getPrescriptionByAppointment(this.appointmentId)
    return loader
      .then((rx) => {
        if (!rx) {
          toast('暂无处方')
          return
        }
        const list = (rx.items || []).map((item) => ({
          label: `${item.medicineName} ${item.spec || ''}`,
          value: `${item.dosage || ''} ${item.frequency || ''} ×${item.quantity}  ¥${item.amount}`
        }))
        this.setData({
          prescription: rx,
          items: list,
          amountText: formatFee(rx.totalAmount),
          statusText: ({ unpaid: '待支付', paid: '待取药', dispensed: '已取药', cancelled: '已取消' })[rx.status] || rx.status,
          canPay: rx.status === 'unpaid',
          canPickup: rx.status === 'paid'
        })
      })
      .catch((err) => toast(err.message || '加载失败'))
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
    if (!this.data.canPay || this.data.paying) return
    const { payMethod } = this.data
    if (needsPayDrawer(payMethod)) {
      this.setData({ payDrawerVisible: true, payDrawerMethod: payMethod })
      return
    }
    this.doPay()
  },

  async doPay() {
    const { prescription, payMethod, balance } = this.data
    if (!prescription || this.data.paying) return
    if (payMethod === 'balance' && Number(prescription.totalAmount) > Number(balance)) {
      toast('余额不足，请先充值')
      return
    }
    this.setData({ paying: true })
    try {
      await mockChannelPay(payMethod)
      await api.payPrescription(prescription.id, payMethod)
      await auth.fetchMe()
      this.setData({ payDrawerVisible: false })
      wx.showToast({ title: '支付成功，请完成取药确认', icon: 'success' })
      this.loadData()
      this.loadBalance()
    } catch (err) {
      toast(err.message || '支付失败')
    } finally {
      this.setData({ paying: false })
    }
  },

  onPickup() {
    if (!this.data.canPickup || this.data.pickuping || !this.data.prescription) return
    wx.showModal({
      title: '确认取药',
      content: '请确认你已从药房领取处方中的药品。确认后，本次就诊将标记为已完成。',
      success: (result) => { if (result.confirm) this.doPickup() }
    })
  },

  async doPickup() {
    const prescription = this.data.prescription
    if (!prescription || this.data.pickuping) return
    this.setData({ pickuping: true })
    try {
      await api.pickupPrescription(prescription.id)
      wx.showToast({ title: '取药已确认', icon: 'success' })
      await this.loadData()
    } catch (err) {
      toast(err.message || '取药确认失败')
    } finally {
      this.setData({ pickuping: false })
    }
  }
})
