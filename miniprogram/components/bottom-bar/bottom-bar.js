Component({
  properties: {
    text: { type: String, value: '确定' },
    disabled: { type: Boolean, value: false },
    loading: { type: Boolean, value: false },
    price: { type: String, value: '' },
    priceLabel: { type: String, value: '' },
    aboveTab: { type: Boolean, value: false }
  },
  methods: {
    onTap() {
      if (this.data.disabled || this.data.loading) return
      this.triggerEvent('confirm')
    }
  }
})
