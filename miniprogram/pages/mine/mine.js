const api = require('../../utils/api')
const auth = require('../../utils/auth')
const toast = require('../../utils/toast')
const { setTabBar } = require('../../utils/tab')
const { resolveMediaUrl } = require('../../utils/media')

Page({
  data: {
    loggedIn: false,
    user: null,
    displayAvatar: '',
    balanceText: '',
    wxNickname: '',
    wxAvatarPath: '',
    wxLoginLoading: false,
    profileSyncing: false,
    unreadCount: 0,
    menuItems: [
      { icon: 'bell', title: '消息通知', url: '/pages/mine/notifications/notifications', badge: true },
      { icon: 'balance-o', title: '余额充值', url: '/pages/recharge/recharge' },
      { icon: 'orders-o', title: '我的订单', url: '/pages/order/list', tab: true },
      { icon: 'description', title: '用户协议', url: '/pages/mine/agreement/agreement' },
      { icon: 'shield-o', title: '隐私政策', url: '/pages/mine/privacy/privacy' },
      { icon: 'info-o', title: '关于我们', url: '/pages/mine/about/about' }
    ]
  },

  onShow() {
    setTabBar(this, '/pages/mine/mine')
    this.syncUser()
    this.loadUnread()
  },

  loadUnread() {
    if (!auth.isLoggedIn()) {
      this.setData({ unreadCount: 0 })
      return
    }
    api.getUnreadNotificationCount()
      .then((count) => this.setData({ unreadCount: count || 0 }))
      .catch(() => {})
  },

  syncUser() {
    const user = auth.getUser()
    const avatar = user && user.avatar ? resolveMediaUrl(user.avatar) : ''
    const balanceText = user && user.balance != null ? Number(user.balance).toFixed(2) : ''
    this.setData({
      loggedIn: auth.isLoggedIn(),
      user,
      displayAvatar: this.data.wxAvatarPath || avatar,
      balanceText,
      wxNickname: user ? (user.nickname || '') : this.data.wxNickname
    })
  },

  onChooseAvatar(e) {
    const path = e.detail.avatarUrl
    if (!path) return
    this.setData({
      wxAvatarPath: path,
      displayAvatar: path
    })
    if (this.data.loggedIn) {
      this.syncProfile({ avatarPath: path })
    }
  },

  onNicknameInput(e) {
    this.setData({ wxNickname: e.detail.value })
  },

  onNicknameBlur(e) {
    if (!this.data.loggedIn) return
    const nickname = (e.detail.value || '').trim()
    const current = (this.data.user && this.data.user.nickname) || ''
    if (!nickname || nickname === current) return
    this.syncProfile({ nickname })
  },

  syncProfile(payload) {
    if (this.data.profileSyncing) return
    this.setData({ profileSyncing: true })
    auth.syncWxProfile(payload)
      .then(() => {
        this.setData({ wxAvatarPath: '' })
        this.syncUser()
        toast.success('资料已更新')
      })
      .catch((err) => toast(err.message || '资料更新失败'))
      .finally(() => this.setData({ profileSyncing: false }))
  },

  onWxLogin() {
    if (this.data.wxLoginLoading) return
    const nickname = (this.data.wxNickname || '').trim()
    this.setData({ wxLoginLoading: true })
    auth.wxLogin({
      nickname,
      avatarPath: this.data.wxAvatarPath || ''
    })
      .then(() => {
        this.setData({ wxAvatarPath: '' })
        this.syncUser()
        toast.success('登录成功')
      })
      .catch((err) => toast(err.message || '微信登录失败'))
      .finally(() => this.setData({ wxLoginLoading: false }))
  },

  onLogout() {
    auth.logout()
    this.setData({ wxNickname: '', wxAvatarPath: '', displayAvatar: '' })
    this.syncUser()
    toast('已退出登录')
  },

  onMenuTap(e) {
    const { item } = e.currentTarget.dataset
    if (item.tab) {
      wx.switchTab({ url: item.url })
    } else {
      wx.navigateTo({ url: item.url })
    }
  }
})
