Component({
  properties: {
    list: { type: Array, value: [] },
    value: { type: String, value: '' }
  },
  methods: {
    onSelect(e) {
      const { value } = e.currentTarget.dataset
      this.triggerEvent('change', { value })
    }
  }
})
