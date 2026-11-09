const { request } = require('./request')

function recharge(amount, payMethod) {
  return request({
    url: '/api/wallet/recharge',
    method: 'POST',
    data: { amount, payMethod }
  })
}

function fetchBalanceLogs(params = {}) {
  return request({
    url: '/api/wallet/logs',
    data: {
      current: params.current || 1,
      size: params.size || 20
    }
  })
}

module.exports = {
  recharge,
  fetchBalanceLogs
}
