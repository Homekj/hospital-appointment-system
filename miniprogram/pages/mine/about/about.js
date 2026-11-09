const { aboutInfo } = require('../../../utils/legal-content')

Page({
  data: {
    info: aboutInfo
  },

  onCallPhone() {
    const phone = this.data.info.hospital.phone
    if (!phone) return
    wx.makePhoneCall({ phoneNumber: phone.replace(/-/g, '') })
  },

  onOpenAgreement() {
    wx.navigateTo({ url: '/pages/mine/agreement/agreement' })
  },

  onOpenPrivacy() {
    wx.navigateTo({ url: '/pages/mine/privacy/privacy' })
  }
})
