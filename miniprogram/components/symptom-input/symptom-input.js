Component({
  properties: {
    value: { type: String, value: '' },
    maxLength: { type: Number, value: 200 }
  },
  methods: {
    onInput(e) {
      this.triggerEvent('change', { value: e.detail.value })
    }
  }
})
