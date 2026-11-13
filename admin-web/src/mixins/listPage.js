/**
 * 列表页通用逻辑：分页、查询、新增/编辑弹窗、删除确认。
 *
 * 页面只需要实现：
 *   fetchList(params)          -> Promise<{records,total,current,size}>
 *   saveData(form, isEdit, id) -> Promise
 *   deleteData(row)            -> Promise（需要删除按钮时）
 *   defaultQuery() / defaultForm() / pageCreated()（可选）
 *
 * 分页完全由后端承担：每次翻页都把 current / size 发给后端，
 * 表格只渲染当前页 records，不在前端对全量数据切片。
 */
export default {
  data() {
    return {
      rows: [],
      total: 0,
      current: 1,
      size: 10,
      loading: false,
      query: {},
      dialogVisible: false,
      dialogMode: 'create',
      editId: null,
      form: {},
      submitting: false
    }
  },
  created() {
    if (this.defaultQuery) this.query = this.defaultQuery()
    if (this.defaultForm) this.form = this.defaultForm()
    if (this.pageCreated) this.pageCreated()
    this.loadData()
  },
  methods: {
    cleanParams(source) {
      const result = {}
      Object.keys(source || {}).forEach(key => {
        const value = source[key]
        if (value !== '' && value !== null && value !== undefined) {
          result[key] = value
        }
      })
      return result
    },
    pageParams(extra) {
      return Object.assign({ current: this.current, size: this.size }, this.cleanParams(this.query), extra || {})
    },
    async loadData() {
      this.loading = true
      try {
        const page = await this.fetchList(this.pageParams())
        this.rows = (page && page.records) || []
        this.total = Number((page && page.total) || 0)
        if (page && page.current) this.current = Number(page.current)
        if (page && page.size) this.size = Number(page.size)
      } catch (e) {
        this.rows = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.current = 1
      this.loadData()
    },
    resetQuery() {
      this.query = this.defaultQuery ? this.defaultQuery() : {}
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.size = size
      this.current = 1
      this.loadData()
    },
    handleCurrentChange(page) {
      this.current = page
      this.loadData()
    },
    openCreate() {
      this.dialogMode = 'create'
      this.editId = null
      this.form = this.defaultForm ? this.defaultForm() : {}
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.editId = row.id
      this.form = Object.assign({}, this.defaultForm ? this.defaultForm() : {}, row)
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    closeDialog() {
      this.dialogVisible = false
    },
    submitForm() {
      if (!this.$refs.form) return
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const isEdit = this.dialogMode === 'edit'
        this.saveData(this.form, isEdit, this.editId)
          .then(result => {
            this.$message.success(isEdit ? '修改成功' : '新增成功')
            this.dialogVisible = false
            this.loadData()
            if (this.afterSave) this.afterSave(result, isEdit)
          })
          .catch(() => {})
          .finally(() => {
            this.submitting = false
          })
      })
    },
    confirmRemove(row, label) {
      this.$confirm(`确定删除「${label}」吗？此操作不可撤销。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
        .then(() => this.deleteData(row))
        .then(() => {
          this.$message.success('删除成功')
          if (this.rows.length === 1 && this.current > 1) this.current -= 1
          this.loadData()
        })
        .catch(() => {})
    },
    async toggleStatus(row, apiCall) {
      try {
        await apiCall(row.id, row.status === 1 ? 0 : 1)
        this.$message.success(row.status === 1 ? '已停用' : '已启用')
        this.loadData()
      } catch (e) {
        /* 错误提示已由请求拦截器统一处理 */
      }
    }
  }
}
