Component({
  properties: {
    doctor: { type: Object, value: {} },
    showDept: { type: Boolean, value: true }
  },
  observers: {
    doctor(d) {
      if (!d || !this.data.showDept) return
      if (d.departmentName) {
        this.setData({ deptName: d.departmentName })
        return
      }
      if (!d.departmentId) return
      const { getDepartmentById } = require('../../mock/departments')
      const dept = getDepartmentById(d.departmentId)
      this.setData({ deptName: dept ? dept.name : '' })
    }
  },
  data: { deptName: '' },
  methods: {
    onTap() {
      this.triggerEvent('tap', { id: this.data.doctor.id })
    }
  }
})
