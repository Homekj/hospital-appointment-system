Component({
  properties: {
    label: { type: String, value: '' },
    value: { type: String, value: '' },
    placeholder: { type: String, value: '请选择' },
    arrow: { type: Boolean, value: true }
  },
  methods: {
    onTap() {
      this.triggerEvent('tap')
    }
  }
})
