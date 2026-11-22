Page({
  data: {
    symptom: '',
    loading: false,
    quickTags: ['头痛', '发热', '咳嗽', '腹痛', '皮疹', '失眠']
  },

  onShow() {
    const app = getApp()
    if (app.globalData.triageSymptom) {
      this.setData({ symptom: app.globalData.triageSymptom })
      app.globalData.triageSymptom = ''
    }
  },

  onSymptomChange(e) {
    this.setData({ symptom: e.detail.value })
  },

  onQuickTag(e) {
    const tag = e.currentTarget.dataset.tag
    const { symptom } = this.data
    const next = symptom ? `${symptom}、${tag}` : tag
    if (next.length > 200) {
      wx.showToast({ title: '描述过长', icon: 'none' })
      return
    }
    this.setData({ symptom: next })
  },

  onAnalyze() {
    const { symptom, loading } = this.data
    if (loading) return
    const text = symptom.trim()
    if (!text) {
      wx.showToast({ title: '请描述您的症状', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    const api = require('../../utils/api')
    api.triageAnalyze(text)
      .then((result) => {
        const app = getApp()
        app.globalData.triageResult = result
        wx.navigateTo({ url: '/pages/triage/result' })
      })
      .catch((err) => {
        wx.showToast({ title: err.message || '分析失败', icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  }
})
