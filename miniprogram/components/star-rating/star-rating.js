Component({
  properties: {
    value: { type: Number, value: 5 },
    readonly: { type: Boolean, value: false },
    size: { type: Number, value: 20 }
  },
  methods: {
    onChange(e) {
      if (this.data.readonly) return
      this.triggerEvent('change', { value: e.detail })
    }
  }
})
