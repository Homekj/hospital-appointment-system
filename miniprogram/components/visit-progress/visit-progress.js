const { VISIT_STEPS, getVisitStepIndex } = require('../../utils/format')

Component({
  properties: {
    status: { type: String, value: '' }
  },
  observers: {
    status(s) {
      const active = getVisitStepIndex(s)
      const steps = VISIT_STEPS.map((item, index) => ({
        ...item,
        done: active >= 0 && index < active,
        active: active >= 0 && index === active
      }))
      this.setData({ steps, hidden: active < 0 || s === 'unpaid' || s === 'cancelled' })
    }
  },
  data: {
    steps: [],
    hidden: true
  }
})
