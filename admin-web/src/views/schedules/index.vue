<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">排班管理</h2>
        <p class="page-desc">号源按医生 + 日期 + 时段唯一，支持单条维护与批量生成，对接 /api/admin/schedules</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-select v-model="query.doctorId" placeholder="医生" clearable filterable style="width: 180px">
            <el-option v-for="d in doctorOptions" :key="d.id" :label="`${d.name}（${d.departmentName || '未分科'}）`" :value="d.id" />
          </el-select>
          <el-date-picker v-model="query.startDate" type="date" value-format="yyyy-MM-dd" placeholder="开始日期" style="width: 160px" />
          <el-date-picker v-model="query.endDate" type="date" value-format="yyyy-MM-dd" placeholder="结束日期" style="width: 160px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="停诊" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button icon="el-icon-s-grid" @click="openBatch">批量生成</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增排班</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="75" />
        <el-table-column prop="doctorName" label="医生" min-width="110" />
        <el-table-column prop="departmentName" label="科室" min-width="100" />
        <el-table-column prop="scheduleDate" label="出诊日期" width="130" />
        <el-table-column prop="timeSlot" label="时段" width="90" align="center" />
        <el-table-column prop="totalQuota" label="总号源" width="90" align="center" />
        <el-table-column prop="bookedCount" label="已预约" width="90" align="center" />
        <el-table-column label="剩余" width="90" align="center">
          <template slot-scope="{ row }">
            <span :class="{ muted: remain(row) <= 0 }">{{ remain(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '正常' : '停诊' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" :icon="row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="toggle(row)">
              {{ row.status === 1 ? '停诊' : '恢复' }}
            </el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-link" @click="confirmRemove(row, `${row.doctorName} ${row.scheduleDate} ${row.timeSlot}`)">删除</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无排班数据</template>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="current"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog :title="dialogMode === 'create' ? '新增排班' : '编辑排班'" :visible.sync="dialogVisible" width="560px" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="请选择医生" filterable style="width: 100%">
            <el-option v-for="d in doctorOptions" :key="d.id" :label="`${d.name}（${d.departmentName || '未分科'}）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出诊日期" prop="scheduleDate">
          <el-date-picker v-model="form.scheduleDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-select v-model="form.timeSlot" placeholder="选择时段" style="width: 100%">
            <el-option v-for="s in slots" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="总号源" prop="totalQuota">
          <el-input-number v-model="form.totalQuota" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">停诊</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="批量生成排班" :visible.sync="batchVisible" width="600px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
        title="按所选医生 × 日期区间 × 时段生成；已存在的排班会自动跳过，不会覆盖已有号源。" />
      <el-form ref="batchForm" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="医生" prop="doctorIds">
          <el-select v-model="batchForm.doctorIds" multiple filterable collapse-tags placeholder="可多选" style="width: 100%">
            <el-option v-for="d in doctorOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期区间" prop="dateRange">
          <el-date-picker v-model="batchForm.dateRange" type="daterange" value-format="yyyy-MM-dd"
            range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlots">
          <el-select v-model="batchForm.timeSlots" multiple placeholder="可多选" style="width: 100%">
            <el-option v-for="s in slots" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="每时段号源" prop="totalQuota">
          <el-input-number v-model="batchForm.totalQuota" :min="1" :max="1000" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batching" @click="submitBatch">生成</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { scheduleApi, catalogApi } from '@/api/admin'

export default {
  name: 'SchedulesPage',
  mixins: [listPage],
  data() {
    return {
      slots: ['08:00', '09:00', '10:00', '14:00', '15:00', '16:00'],
      doctorOptions: [],
      batchVisible: false,
      batching: false,
      batchForm: { doctorIds: [], dateRange: [], timeSlots: [], totalQuota: 5 },
      rules: {
        doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
        scheduleDate: [{ required: true, message: '请选择出诊日期', trigger: 'change' }],
        timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }],
        totalQuota: [{ required: true, message: '请填写总号源', trigger: 'blur' }]
      },
      batchRules: {
        doctorIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一位医生', trigger: 'change' }],
        dateRange: [{ required: true, type: 'array', min: 2, message: '请选择日期区间', trigger: 'change' }],
        timeSlots: [{ required: true, type: 'array', min: 1, message: '请至少选择一个时段', trigger: 'change' }]
      }
    }
  },
  methods: {
    defaultQuery() { return { doctorId: '', startDate: '', endDate: '', status: '' } },
    defaultForm() { return { doctorId: '', scheduleDate: '', timeSlot: '09:00', totalQuota: 5, status: 1 } },
    pageCreated() {
      catalogApi.doctors().then(list => { this.doctorOptions = list || [] }).catch(() => {})
    },
    fetchList(params) { return scheduleApi.page(params) },
    saveData(form, isEdit, id) { return isEdit ? scheduleApi.update(id, form) : scheduleApi.create(form) },
    deleteData(row) { return scheduleApi.remove(row.id) },
    toggle(row) { this.toggleStatus(row, scheduleApi.status) },
    remain(row) {
      if (row.remainQuota !== undefined && row.remainQuota !== null) return row.remainQuota
      return Number(row.totalQuota || 0) - Number(row.bookedCount || 0)
    },
    openBatch() {
      this.batchForm = { doctorIds: [], dateRange: [], timeSlots: ['09:00'], totalQuota: 5 }
      this.batchVisible = true
      this.$nextTick(() => { if (this.$refs.batchForm) this.$refs.batchForm.clearValidate() })
    },
    submitBatch() {
      this.$refs.batchForm.validate(valid => {
        if (!valid) return
        const range = this.batchForm.dateRange || []
        this.batching = true
        scheduleApi.batch({
          doctorIds: this.batchForm.doctorIds,
          startDate: range[0],
          endDate: range[1],
          timeSlots: this.batchForm.timeSlots,
          totalQuota: this.batchForm.totalQuota
        })
          .then(() => {
            this.$message.success('批量排班已完成')
            this.batchVisible = false
            this.loadData()
          })
          .catch(() => {})
          .finally(() => { this.batching = false })
      })
    }
  }
}
</script>
