const TAB_INDEX = {
  '/pages/index/index': 0,
  '/pages/schedule/list': 1,
  '/pages/order/list': 2,
  '/pages/mine/mine': 3
}

function setTabBar(page, path) {
  if (typeof page.getTabBar !== 'function') return
  const tabBar = page.getTabBar()
  if (!tabBar) return
  const active = TAB_INDEX[path]
  if (active !== undefined) {
    tabBar.setData({ active })
  }
}

function goDeptTab(options = {}) {
  const app = getApp()
  if (options.deptId) {
    app.globalData.pendingDeptId = Number(options.deptId)
  }
  if ('keyword' in options) {
    app.globalData.pendingKeyword = (options.keyword || '').trim()
  }
  wx.switchTab({ url: '/pages/schedule/list' })
}

module.exports = { setTabBar, goDeptTab, TAB_INDEX }
