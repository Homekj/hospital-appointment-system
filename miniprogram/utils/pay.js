const PAY_METHODS = [
  { id: 'wechat', name: '微信支付', desc: '模拟支付', icon: 'wechat', logo: '' },
  { id: 'alipay', name: '支付宝', desc: '模拟支付', icon: 'alipay', logo: '' },
  { id: 'balance', name: '余额支付', desc: '', icon: 'balance', logo: '' }
]

const PAY_CHANNEL_INFO = {
  wechat: { name: '微信支付', logo: '' },
  alipay: { name: '支付宝', logo: '' },
  balance: { name: '余额支付', logo: '' }
}

function payMethodLabel(method) {
  const map = {
    balance: '余额支付',
    wechat: '微信支付',
    alipay: '支付宝'
  }
  return map[method] || method || ''
}

function getPayChannelInfo(method) {
  return PAY_CHANNEL_INFO[method] || PAY_CHANNEL_INFO.wechat
}

function isChannelPay(method) {
  return method === 'wechat' || method === 'alipay'
}

function needsPayDrawer(method) {
  return method === 'wechat' || method === 'alipay' || method === 'balance'
}

function mockChannelPay(payMethod) {
  return new Promise((resolve) => {
    if (payMethod === 'balance') {
      resolve()
      return
    }
    setTimeout(() => resolve(), 800)
  })
}

module.exports = {
  PAY_METHODS,
  PAY_CHANNEL_INFO,
  payMethodLabel,
  getPayChannelInfo,
  isChannelPay,
  needsPayDrawer,
  mockChannelPay
}
