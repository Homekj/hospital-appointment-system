const api = require('../../../utils/api')
const toast = require('../../../utils/toast')

const relationships = ['本人', '父母', '子女', '配偶', '其他']
const emptyForm = () => ({ name: '', phone: '', relationship: '本人', defaultPatient: false })

Page({
  data: { profiles: [], relationships, form: emptyForm(), relationIndex: 0, busy: false, error: '' },

  onShow() { this.load() },

  async load() {
    try {
      this.setData({ profiles: await api.getPatients(), error: '' })
    } catch (error) {
      this.setData({ error: error.message || '就诊人加载失败' })
    }
  },

  edit(e) {
    const form = this.data.profiles.find(p => p.id === e.currentTarget.dataset.id)
    if (form) this.setData({ form: { ...form }, relationIndex: relationships.indexOf(form.relationship) })
  },

  reset() { this.setData({ form: emptyForm(), relationIndex: 0 }) },

  changeName(e) { this.setData({ 'form.name': e.detail }) },
  changePhone(e) { this.setData({ 'form.phone': e.detail }) },
  changeDefault(e) { this.setData({ 'form.defaultPatient': e.detail.value }) },
  changeRelationship(e) {
    const index = Number(e.detail.value)
    this.setData({ 'form.relationship': relationships[index], relationIndex: index })
  },

  async save() {
    if (this.data.busy) return
    const form = { ...this.data.form, name: this.data.form.name.trim() }
    if (!form.name || !/^1[3-9]\d{9}$/.test(form.phone)) {
      toast('请填写姓名和有效手机号')
      return
    }
    this.setData({ busy: true })
    try {
      this.setData({ profiles: await api.savePatient(form), error: '' })
      this.reset()
      toast('已保存')
    } catch (error) {
      toast(error.message || '保存失败')
    } finally {
      this.setData({ busy: false })
    }
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除常用就诊人',
      content: '已生成的预约会保留原有就诊信息。',
      success: async ({ confirm }) => {
        if (!confirm || this.data.busy) return
        this.setData({ busy: true })
        try {
          this.setData({ profiles: await api.removePatient(id), error: '' })
          if (this.data.form.id === id) this.reset()
        } catch (error) {
          toast(error.message || '删除失败')
        } finally {
          this.setData({ busy: false })
        }
      }
    })
  }
})
