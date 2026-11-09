const api = require('../../../utils/api')
const auth = require('../../../utils/auth')
const toast = require('../../../utils/toast')
const { formatServerDateTime } = require('../../../utils/format')

Page({
  data: {
    loading: false,
    list: []
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      toast('请先登录')
      wx.switchTab({ url: '/pages/mine/mine' })
      return
    }
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.getNotifications({ current: 1, size: 50 })
      .then((page) => {
        const list = (page.records || []).map((item) => ({
          ...item,
          timeText: formatServerDateTime(item.createTime)
        }))
        this.setData({ list })
      })
      .catch((err) => toast(err.message || '加载失败'))
      .finally(() => this.setData({ loading: false }))
  },

  onReadAll() {
    api.markAllNotificationsRead()
      .then(() => {
        wx.showToast({ title: '已全部已读', icon: 'success' })
        this.loadList()
      })
      .catch((err) => toast(err.message || '操作失败'))
  },

  onItemTap(e) {
    const { item } = e.currentTarget.dataset
    if (!item) return
    const go = () => {
      if (item.bizType === 'appointment' && item.bizId) {
        wx.navigateTo({ url: `/pages/order/detail?id=${item.bizId}` })
      }
    }
    if (!item.read) {
      api.markNotificationRead(item.id)
        .then(go)
        .catch(go)
    } else {
      go()
    }
  }
})
