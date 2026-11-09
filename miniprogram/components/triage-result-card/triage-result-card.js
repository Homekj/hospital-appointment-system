Component({
  properties: {
    department: { type: Object, value: {} },
    rank: { type: Number, value: 1 }
  },
  methods: {
    onTap() {
      this.triggerEvent('select', { id: this.data.department.id })
    }
  }
})
