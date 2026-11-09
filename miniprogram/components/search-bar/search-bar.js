Component({
  properties: {
    placeholder: { type: String, value: '搜索科室、医生' },
    value: { type: String, value: '' }
  },
  observers: {
    value(val) {
      this.setData({ inputValue: val || '' })
    }
  },
  data: {
    inputValue: ''
  },
  methods: {
    onInput(e) {
      const value = e.detail.value
      this.setData({ inputValue: value })
      this.triggerEvent('change', { value })
    },
    onConfirm(e) {
      this.triggerEvent('search', { value: e.detail.value })
    },
    onSearchTap() {
      this.triggerEvent('search', { value: this.data.inputValue })
    }
  }
})
