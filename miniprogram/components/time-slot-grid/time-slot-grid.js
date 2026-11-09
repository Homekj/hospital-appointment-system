Component({
  properties: {
    morning: { type: Array, value: [] },
    afternoon: { type: Array, value: [] },
    selected: { type: String, value: '' },
    hasAnySchedule: { type: Boolean, value: false },
    hasAvailable: { type: Boolean, value: false }
  },
  methods: {
    onSelect(e) {
      const { time, available, scheduleId } = e.currentTarget.dataset
      if (!available) return
      this.triggerEvent('select', { time, scheduleId })
    }
  }
})
