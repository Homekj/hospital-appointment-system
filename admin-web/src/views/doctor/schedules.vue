<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的排班</h2>
        <p class="page-desc">只看本人排班（身份来自 JWT），对接 GET /api/doctor/schedules，最多返回 200 条</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-date-picker v-model="query.startDate" type="date" value-format="yyyy-MM-dd" placeholder="开始日期" style="width: 160px" />
          <el-date-picker v-model="query.endDate" type="date" value-format="yyyy-MM-dd" placeholder="结束日期" style="width: 160px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="停诊" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
          <el-button icon="el-icon-date" @click="thisMonth">本月</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="排班 ID" width="90" />
        <el-table-column label="出诊日期" width="130">
          <template slot-scope="{ row }">{{ row.scheduleDate | date }}</template>
        </el-table-column>
        <el-table-column prop="timeSlot" label="时段" width="90" align="center" />
        <el-table-column prop="totalQuota" label="总号源" width="100" align="center" />
        <el-table-column prop="bookedCount" label="已预约" width="100" align="center" />
        <el-table-column prop="remainQuota" label="剩余号源" width="100" align="center" />
        <el-table-column prop="appointmentCount" label="有效预约数" width="110" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" effect="plain" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停诊' }}</el-tag>
          </template>
        </el-table-column>
        <template slot="empty">所选条件下无排班</template>
      </el-table>
      <div class="pagination"><span class="muted">共 {{ rows.length }} 条（不分页，后端按日期区间返回）</span></div>
    </div>
  </div>
</template>

<script>
import { doctorApi } from '@/api/doctor'

function firstDayOfMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}
function lastDayOfMonth() {
  const d = new Date()
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0)
  return `${last.getFullYear()}-${String(last.getMonth() + 1).padStart(2, '0')}-${String(last.getDate()).padStart(2, '0')}`
}

export default {
  name: 'DoctorSchedules',
  data() {
    return {
      rows: [],
      loading: false,
      query: { startDate: firstDayOfMonth(), endDate: lastDayOfMonth(), status: '' }
    }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      const params = {}
      Object.keys(this.query).forEach(key => {
        if (this.query[key] !== '' && this.query[key] !== null) params[key] = this.query[key]
      })
      doctorApi.schedules(params)
        .then(list => { this.rows = list || [] })
        .catch(() => { this.rows = [] })
        .finally(() => { this.loading = false })
    },
    reset() {
      this.query = { startDate: '', endDate: '', status: '' }
      this.load()
    },
    thisMonth() {
      this.query.startDate = firstDayOfMonth()
      this.query.endDate = lastDayOfMonth()
      this.load()
    }
  }
}
</script>
