const { getPayChannelInfo } = require('../../utils/pay')

Component({
  properties: {
    show: { type: Boolean, value: false },
    method: { type: String, value: 'wechat' },
    amount: { type: String, value: '0.00' },
    balance: { type: String, value: '' },
    loading: { type: Boolean, value: false }
  },

  data: {
    channelName: ''
  },

  observers: {
    method(method) {
      const info = getPayChannelInfo(method)
      this.setData({ channelName: info.name })
    }
  },

  lifetimes: {
    attached() {
      const info = getPayChannelInfo(this.data.method)
      this.setData({ channelName: info.name })
    }
  },

  methods: {
    onClose() {
      this.triggerEvent('close')
    },

    onConfirm() {
      if (this.data.loading) return
      this.triggerEvent('confirm')
    }
  }
})
