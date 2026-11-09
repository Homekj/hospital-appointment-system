function getStatusBarHeight() {
  try {
    return wx.getWindowInfo().statusBarHeight || 20
  } catch (e) {
    return 20
  }
}

function getNavBarHeight() {
  try {
    const win = wx.getWindowInfo()
    const navContentPx = Math.round(88 * win.windowWidth / 750)
    return (win.statusBarHeight || 20) + navContentPx
  } catch (e) {
    return 64
  }
}

module.exports = { getStatusBarHeight, getNavBarHeight }
