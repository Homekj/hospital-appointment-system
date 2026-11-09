Component({
  properties: {
    doctor: { type: Object, value: {} }
  },
  observers: {
    doctor(d) {
      if (!d) return
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
  data: { deptName: '' }
})
