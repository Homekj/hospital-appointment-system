const auth = require('./utils/auth')
const { setupPrivacyAuthorization } = require('./utils/privacy')

App({
  globalData: {
    token: '',
    user: null,
    triageSymptom: '',
    triageResult: null,
    pendingDeptId: 0,
    pendingKeyword: ''
  },

  onLaunch() {
    setupPrivacyAuthorization()

    const token = auth.getToken()
    const user = auth.getUser()
    if (token) {
      this.globalData.token = token
      this.globalData.user = user
      auth.fetchMe().catch(() => {
        auth.sessionExpired()
      })
    } else if (auth.shouldAutoLogin()) {
      auth.wxLogin().catch(() => {})
    }
  }
})
