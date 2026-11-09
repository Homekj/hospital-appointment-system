Component({
  properties: {
    title: { type: String, value: '' },
    more: { type: Boolean, value: false }
  },
  methods: {
    onMore() {
      this.triggerEvent('more')
    }
  }
})
