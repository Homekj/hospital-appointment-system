Component({
  options: {
    addGlobalClass: true
  },
  properties: {
    title: {
      type: String,
      value: ''
    },
    back: {
      type: Boolean,
      value: true
    }
  },
  methods: {
    onBack() {
      if (!this.data.back) return
      wx.navigateBack({
        delta: 1,
        fail: () => {
          wx.switchTab({ url: '/pages/index/index' })
        }
      })
    }
  }
})
