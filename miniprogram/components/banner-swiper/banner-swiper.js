Component({
  properties: {
    banners: {
      type: Array,
      value: [
        {
          id: 2,
          type: 'image',
          image: '/assets/banner/banner2.svg',
          link: 'dept'
        },
        {
          id: 3,
          type: 'image',
          image: '/assets/banner/banner3.svg',
          link: 'triage'
        }
      ]
    }
  },
  data: { current: 0 },
  methods: {
    onChange(e) {
      this.setData({ current: e.detail.current })
    },
    onBannerTap(e) {
      const { link } = e.currentTarget.dataset
      if (link === 'triage') {
        wx.navigateTo({ url: '/pages/triage/index' })
        return
      }
      if (link === 'dept') {
        const { goDeptTab } = require('../../utils/tab')
        goDeptTab()
      }
    }
  }
})
