Component({
  properties: {
    list: { type: Array, value: [] }
  },
  methods: {
    onTap(e) {
      const { id } = e.currentTarget.dataset
      this.triggerEvent('select', { id })
    }
  }
})
