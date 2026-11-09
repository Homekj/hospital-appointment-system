function setupPrivacyAuthorization() {
  if (typeof wx.onNeedPrivacyAuthorization !== 'function') {
    return
  }
  wx.onNeedPrivacyAuthorization((resolve) => {
    wx.showModal({
      title: '隐私提示',
      content: '部分能力需要您同意《用户隐私保护指引》后方可使用。',
      confirmText: '同意',
      cancelText: '拒绝',
      success(res) {
        if (res.confirm) {
          resolve({ event: 'agree', buttonId: 'agree-privacy-btn' })
        }
      }
    })
  })
}

module.exports = { setupPrivacyAuthorization }
