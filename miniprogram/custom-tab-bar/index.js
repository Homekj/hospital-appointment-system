Component({
  data: {
    active: 0,
    routes: [
      '/pages/index/index',
      '/pages/schedule/list',
      '/pages/order/list',
      '/pages/mine/mine'
    ]
  },

  methods: {
    onChange(e) {
      const idx = Number(e.detail)
      const url = this.data.routes[idx]
      if (!url || idx === this.data.active) return
      wx.switchTab({ url })
    }
  }
})
