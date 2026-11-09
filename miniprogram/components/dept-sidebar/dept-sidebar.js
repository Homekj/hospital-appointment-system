Component({
  properties: {
    list: { type: Array, value: [] },
    activeId: { type: Number, value: 0 }
  },
  methods: {
    onSelect(e) {
      const { id } = e.currentTarget.dataset
      this.triggerEvent('change', { id: Number(id) })
    }
  }
})
