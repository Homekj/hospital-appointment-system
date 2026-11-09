const { getStatusInfo } = require('../../utils/format')

Component({
  properties: {
    status: { type: String, value: '' },
    location: { type: String, value: '' }
  },
  observers: {
    status(s) {
      const info = getStatusInfo(s)
      this.setData({ statusText: info.text })
    }
  },
  data: { statusText: '' }
})
