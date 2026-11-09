const { getStatusInfo, formatDateTime, formatFee } = require('../../utils/format')

Component({
  properties: {
    order: { type: Object, value: {} }
  },
  observers: {
    order(o) {
      if (!o) return
      const status = getStatusInfo(o.displayStatus || o.rawStatus || o.status)
      this.setData({
        statusText: status.text,
        statusType: status.type,
        timeText: formatDateTime(o.scheduleDate, o.timeSlot),
        feeText: formatFee(o.fee)
      })
    }
  },
  data: {
    statusText: '',
    statusType: 'default',
    timeText: '',
    feeText: ''
  },
  methods: {
    onTap() {
      this.triggerEvent('tap', { id: this.data.order.id })
    },
    onAction(e) {
      const { action } = e.currentTarget.dataset
      this.triggerEvent('action', { action, id: this.data.order.id })
    }
  }
})
