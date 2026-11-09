const auth = require('../../utils/auth')
const walletApi = require('../../utils/wallet-api')
const { PAY_METHODS, mockChannelPay } = require('../../utils/pay')
const toast = require('../../utils/toast')
const { formatServerDateTime } = require('../../utils/format')

const RECHARGE_PAY_METHODS = PAY_METHODS.filter((item) => item.id === 'wechat' || item.id === 'alipay')

function formatBalance(user) {
  if (!user || user.balance == null) return '0.00'
  return Number(user.balance).toFixed(2)
}

Page({
  data: {
    balanceText: '0.00',
    amountOptions: [50, 100, 200, 500],
    selectedAmount: 100,
    customAmount: '',
    rechargeAmount: '100.00',
    payMethods: RECHARGE_PAY_METHODS,
    payMethod: 'wechat',
    payDrawerVisible: false,
    payDrawerMethod: 'wechat',
    submitting: false,
    logs: [],
    logsLoading: false
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      toast('请先登录')
      setTimeout(() => wx.switchTab({ url: '/pages/mine/mine' }), 500)
      return
    }
    this.refreshBalance()
    this.loadLogs()
  },

  async refreshBalance() {
    try {
      const user = await auth.fetchMe()
      this.setData({ balanceText: formatBalance(user) })
    } catch (e) {
      toast(e.message || '加载余额失败')
    }
  },

  async loadLogs() {
    this.setData({ logsLoading: true })
    try {
      const result = await walletApi.fetchBalanceLogs({ current: 1, size: 10 })
      const logs = (result.records || [])
        .filter((item) => item.type === 'recharge')
        .map((item) => ({
          ...item,
          amountText: Number(Math.abs(item.amount)).toFixed(2),
          createTimeText: formatServerDateTime(item.createTime)
        }))
      this.setData({ logs })
    } catch (e) {
      this.setData({ logs: [] })
    } finally {
      this.setData({ logsLoading: false })
    }
  },

  updateRechargeAmount() {
    const { selectedAmount, customAmount } = this.data
    let amount = selectedAmount
    if (customAmount) {
      const custom = Number(customAmount)
      if (!Number.isNaN(custom) && custom > 0) amount = custom
    }
    this.setData({ rechargeAmount: Number(amount || 0).toFixed(2) })
  },

  onAmountTap(e) {
    const amount = Number(e.currentTarget.dataset.amount)
    if (!amount) return
    this.setData({ selectedAmount: amount, customAmount: '' })
    this.updateRechargeAmount()
  },

  onCustomAmountChange(e) {
    this.setData({ customAmount: (e.detail || '').trim(), selectedAmount: 0 })
    this.updateRechargeAmount()
  },

  onPayMethodTap(e) {
    const { id } = e.currentTarget.dataset
    if (!id) return
    this.setData({ payMethod: id, payDrawerVisible: true, payDrawerMethod: id })
  },

  onPayDrawerClose() {
    this.setData({ payDrawerVisible: false })
  },

  onPayDrawerConfirm() {
    this.doRecharge()
  },

  onRechargeTap() {
    const amount = Number(this.data.rechargeAmount)
    if (!amount || amount < 0.01) {
      toast('请输入有效充值金额')
      return
    }
    this.setData({ payDrawerVisible: true, payDrawerMethod: this.data.payMethod })
  },

  async doRecharge() {
    if (this.data.submitting) return
    const amount = Number(this.data.rechargeAmount)
    if (!amount || amount < 0.01) {
      toast('请输入有效充值金额')
      return
    }
    if (amount > 10000) {
      toast('单次充值不能超过 10000 元')
      return
    }
    this.setData({ submitting: true })
    try {
      await mockChannelPay(this.data.payMethod)
      await walletApi.recharge(amount, this.data.payMethod)
      const user = await auth.fetchMe()
      this.setData({
        balanceText: formatBalance(user),
        payDrawerVisible: false,
        customAmount: '',
        selectedAmount: 100
      })
      this.updateRechargeAmount()
      toast.success('充值成功')
      this.loadLogs()
    } catch (e) {
      toast(e.message || '充值失败')
    } finally {
      this.setData({ submitting: false })
    }
  }
})
