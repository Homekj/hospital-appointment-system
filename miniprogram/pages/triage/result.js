const { enrichDepartments } = require('../../utils/dept-meta')
const { goDeptTab } = require('../../utils/tab')

Page({
  data: {
    conclusion: '',
    advice: '',
    urgent: false,
    helpPhone: '',
    departments: []
  },

  onLoad() {
    const app = getApp()
    const result = app.globalData.triageResult
    app.globalData.triageResult = null
    if (!result) {
      wx.showToast({ title: '请先填写症状描述', icon: 'none' })
      wx.navigateBack()
      return
    }
    const needsStaff = result.urgent || result.requiresHumanHelp
    this.setData({
      conclusion: result.preliminaryConclusion || result.summary || '请联系医院导诊台',
      advice: result.advice || '科室建议仅供导航。',
      urgent: !!result.urgent,
      helpPhone: result.helpPhone || '',
      departments: needsStaff ? [] : enrichDepartments(result.departments || [])
    })
  },

  onDeptSelect(e) {
    goDeptTab({ deptId: e.detail.id })
  },

  callHelp() {
    const phoneNumber = this.data.urgent ? '120' : this.data.helpPhone.replace(/[^0-9+]/g, '')
    if (phoneNumber) wx.makePhoneCall({ phoneNumber })
  },

  onViewAll() {
    const first = this.data.departments[0]
    if (first) goDeptTab({ deptId: first.id })
  }
})
