Component({
  properties: {
    list: { type: Array, value: [] }
  },
  methods: {
    onTap(e) {
      const { name } = e.currentTarget.dataset
      this.triggerEvent('select', { name })
    }
  }
})
