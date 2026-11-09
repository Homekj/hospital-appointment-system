const api = require('../../utils/api')
const { enrichDepartments, enrichDoctor } = require('../../utils/adapter')
const { setTabBar } = require('../../utils/tab')
const {
  findDepartmentByKeyword,
  filterDepartmentsByKeyword,
  buildDoctorQuery
} = require('../../utils/search')
const toast = require('../../utils/toast')

Page({
  data: {
    departments: [],
    allDepartments: [],
    activeDeptId: 1,
    doctors: [],
    keyword: '',
    isGlobalSearch: false,
    loading: true
  },

  onLoad(options) {
    const keyword = options.keyword ? decodeURIComponent(options.keyword) : ''
    const deptId = Number(options.deptId)
    const updates = { keyword }
    if (deptId) updates.activeDeptId = deptId
    this.setData(updates)
    this.loadDepartments()
  },

  onShow() {
    setTabBar(this, '/pages/schedule/list')
    this.applyPendingTabParams()
  },

  applyPendingTabParams() {
    const app = getApp()
    const { pendingDeptId, pendingKeyword } = app.globalData
    const updates = {}

    if (pendingDeptId) {
      updates.activeDeptId = pendingDeptId
      app.globalData.pendingDeptId = 0
    }
    if (pendingKeyword) {
      updates.keyword = pendingKeyword
      app.globalData.pendingKeyword = ''
    }

    if (!Object.keys(updates).length) return

    this.setData(updates, () => {
      if (this.data.allDepartments.length) {
        this.syncDepartmentsForKeyword()
        this.loadDoctors()
      } else {
        this._pendingReload = true
      }
    })
  },

  loadDepartments() {
    api.getDepartments()
      .then((departments) => {
        const list = enrichDepartments(departments)
        let activeDeptId = this.data.activeDeptId || (list[0] && list[0].id)
        const keyword = (this.data.keyword || '').trim()
        const deptMatch = findDepartmentByKeyword(list, keyword)
        if (deptMatch) activeDeptId = deptMatch.id

        const displayList = keyword
          ? filterDepartmentsByKeyword(list, keyword)
          : list

        this.setData({
          allDepartments: list,
          departments: displayList.length ? displayList : list,
          activeDeptId
        }, () => {
          if (this._pendingReload) {
            this._pendingReload = false
            this.syncDepartmentsForKeyword()
          }
          this.loadDoctors()
        })
      })
      .catch((err) => {
        this.setData({ loading: false })
        toast(err.message || '加载科室失败')
      })
  },

  syncDepartmentsForKeyword() {
    const keyword = (this.data.keyword || '').trim()
    const { allDepartments } = this.data
    if (!keyword) {
      this.setData({ departments: allDepartments })
      return
    }
    const displayList = filterDepartmentsByKeyword(allDepartments, keyword)
    this.setData({
      departments: displayList.length ? displayList : allDepartments
    })
  },

  loadDoctors(isSwitch = false) {
    const { allDepartments, activeDeptId, keyword } = this.data
    const { params, deptMatch, isGlobalSearch } = buildDoctorQuery({
      departments: allDepartments,
      activeDeptId,
      keyword,
      hasAvailableSlot: true
    })

    const updates = { isGlobalSearch }
    if (deptMatch && deptMatch.id !== activeDeptId) {
      updates.activeDeptId = deptMatch.id
    }

    if (!isSwitch) {
      updates.loading = true
    }
    this.setData(updates)

    api.getDoctors(params)
      .then((doctors) => {
        this.setData({
          doctors: (doctors || []).map((d) => enrichDoctor(d, true)),
          loading: false
        })
      })
      .catch((err) => {
        this.setData({ loading: false })
        toast(err.message || '加载医生失败')
      })
  },

  onSearchChange(e) {
    this.setData({ keyword: e.detail.value || '' })
  },

  onSearch(e) {
    const keyword = (e.detail.value || '').trim()
    this.setData({ keyword }, () => {
      this.syncDepartmentsForKeyword()
      const deptMatch = findDepartmentByKeyword(this.data.allDepartments, keyword)
      if (deptMatch) {
        this.setData({ activeDeptId: deptMatch.id }, () => this.loadDoctors())
        return
      }
      this.loadDoctors()
    })
  },

  onDeptChange(e) {
    this.setData({ activeDeptId: e.detail.id, keyword: '' }, () => {
      this.syncDepartmentsForKeyword()
      this.loadDoctors(true)
    })
  },

  onDoctorTap(e) {
    const { id } = e.detail
    wx.navigateTo({ url: `/pages/schedule/detail?doctorId=${id}` })
  }
})
