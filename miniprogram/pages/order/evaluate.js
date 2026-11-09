const api = require('../../utils/api')
const { toOrder } = require('../../utils/adapter')
const toast = require('../../utils/toast')

Page({
  data: {
    order: null,
    rating: 5,
    selectedTags: [],
    comment: '',
    submitting: false
  },

  onLoad(options) {
    if (!options.id) {
      wx.showToast({ title: '订单不存在', icon: 'none' })
      return
    }
    api.getAppointmentById(options.id)
      .then((appt) => {
        const order = toOrder(appt)
        if (order.status !== 'completed') {
          wx.showToast({ title: '仅已完成订单可评价', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 1500)
          return
        }
        if (order.evaluated) {
          wx.showToast({ title: '您已评价过', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 1500)
          return
        }
        this.setData({ order })
      })
      .catch((err) => {
        toast(err.message || '订单不存在')
        setTimeout(() => wx.navigateBack(), 1500)
      })
  },

  onRatingChange(e) {
    this.setData({ rating: e.detail.value })
  },

  onTagsChange(e) {
    this.setData({ selectedTags: e.detail.selected })
  },

  onCommentInput(e) {
    this.setData({ comment: e.detail })
  },

  onSubmit() {
    if (this.data.submitting || !this.data.order) return
    this.setData({ submitting: true })
    api.submitAppointmentReview(this.data.order.id, {
      rating: this.data.rating,
      tags: this.data.selectedTags,
      comment: this.data.comment
    })
      .then(() => {
        wx.showToast({ title: '评价成功', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 800)
      })
      .catch((err) => toast(err.message || '提交失败'))
      .finally(() => this.setData({ submitting: false }))
  }
})
