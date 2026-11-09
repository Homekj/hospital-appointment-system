function toast(message) {
  if (message == null || message === '') {
    return
  }
  wx.showToast({
    title: String(message),
    icon: 'none',
    duration: 2000
  })
}

toast.loading = (message) => {
  wx.showLoading({ title: String(message || '加载中'), mask: true })
}

toast.success = (message) => {
  wx.showToast({ title: String(message), icon: 'success' })
}

toast.clear = () => {
  wx.hideToast()
  wx.hideLoading()
}

module.exports = toast
