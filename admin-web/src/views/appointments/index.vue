<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">预约管理</h2>
        <p class="page-desc">按就诊日期筛选，取消会由后端回滚号源并退款；支持状态流转与 CSV 导出</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.patientName" placeholder="就诊人姓名" clearable style="width: 160px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 150px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="query.departmentId" placeholder="科室" clearable filterable style="width: 150px">
            <el-option v-for="d in departmentOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-date-picker v-model="query.startDate" type="date" value-format="yyyy-MM-dd" placeholder="就诊开始" style="width: 155px" />
          <el-date-picker v-model="query.endDate" type="date" value-format="yyyy-MM-dd" placeholder="就诊结束" style="width: 155px" />
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button icon="el-icon-download" :loading="exporting" @click="exportCsv">导出 CSV</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="patientName" label="就诊人" width="110" />
        <el-table-column label="联系电话" width="140">
          <template slot-scope="{ row }">{{ row.patientPhone | maskPhone }}</template>
        </el-table-column>
        <el-table-column prop="departmentName" label="科室" min-width="100" />
        <el-table-column prop="doctorName" label="医生" min-width="100" />
        <el-table-column prop="scheduleDate" label="就诊日期" width="130" />
        <el-table-column prop="timeSlot" label="时段" width="85" align="center" />
        <el-table-column label="挂号费" width="100" align="right">
          <template slot-scope="{ row }">{{ row.amount | money }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)" effect="plain">
              {{ row.statusLabel || statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="showDetail(row)">详情</el-button>
            <el-button v-if="canCancel(row)" type="text" icon="el-icon-close" class="danger-link" @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status === 'pending'" type="text" icon="el-icon-location" @click="checkIn(row)">签到</el-button>
            <el-dropdown v-if="otherTransitions(row).length" trigger="click" @command="target => changeStatus(row, target)">
              <el-button type="text" icon="el-icon-sort">流转<i class="el-icon-arrow-down el-icon--right" /></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-for="t in otherTransitions(row)" :key="t" :command="t">
                  {{ statusLabel(t) }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
        <template slot="empty">暂无预约数据</template>
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

    <el-dialog title="预约详情" :visible.sync="detailVisible" width="600px">
      <div v-loading="detailLoading" class="detail-list">
        <el-row :gutter="12">
          <el-col :span="12"><p><strong>预约编号</strong>{{ detail.id }}</p></el-col>
          <el-col :span="12"><p><strong>状态</strong>{{ detail.statusLabel || statusLabel(detail.status) }}</p></el-col>
          <el-col :span="12"><p><strong>就诊人</strong>{{ detail.patientName }}</p></el-col>
          <el-col :span="12"><p><strong>联系电话</strong>{{ detail.patientPhone | maskPhone }}</p></el-col>
          <el-col :span="12"><p><strong>科室</strong>{{ detail.departmentName }}</p></el-col>
          <el-col :span="12"><p><strong>医生</strong>{{ detail.doctorName }}</p></el-col>
          <el-col :span="12"><p><strong>就诊日期</strong>{{ detail.scheduleDate }}</p></el-col>
          <el-col :span="12"><p><strong>时段</strong>{{ detail.timeSlot }}</p></el-col>
          <el-col :span="12"><p><strong>挂号费</strong>{{ detail.amount | money }}</p></el-col>
          <el-col :span="12"><p><strong>支付方式</strong>{{ detail.payMethod || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>支付时间</strong>{{ detail.payTime || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>下单时间</strong>{{ detail.createTime }}</p></el-col>
          <el-col :span="24"><p><strong>备注</strong>{{ detail.remark || '-' }}</p></el-col>
        </el-row>
      </div>
      <span slot="footer"><el-button type="primary" @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { appointmentApi, catalogApi } from '@/api/admin'

const TAG_TYPES = {
  unpaid: 'warning', pending: 'primary', checked_in: 'success',
  in_consultation: 'success', pending_rx_pay: 'warning', completed: 'info', cancelled: 'danger'
}

export default {
  name: 'AppointmentsPage',
  mixins: [listPage],
  data() {
    return {
      statusOptions: [],
      departmentOptions: [],
      detailVisible: false,
      detailLoading: false,
      detail: {},
      exporting: false
    }
  },
  methods: {
    defaultQuery() {
      return { patientName: '', status: '', departmentId: '', startDate: '', endDate: '' }
    },
    pageCreated() {
      appointmentApi.statusOptions().then(list => { this.statusOptions = list || [] }).catch(() => {})
      catalogApi.departments().then(list => { this.departmentOptions = list || [] }).catch(() => {})
    },
    fetchList(params) { return appointmentApi.page(params) },
    statusLabel(value) {
      const found = this.statusOptions.find(item => item.value === value)
      return found ? found.label : value
    },
    statusTagType(value) { return TAG_TYPES[value] || 'info' },
    canCancel(row) {
      return ['unpaid', 'pending', 'checked_in'].indexOf(row.status) >= 0
    },
    otherTransitions(row) {
      return (row.allowedTransitions || []).filter(t => t !== 'cancelled')
    },
    showDetail(row) {
      this.detail = {}
      this.detailVisible = true
      this.detailLoading = true
      appointmentApi.detail(row.id)
        .then(data => { this.detail = data || {} })
        .catch(() => {})
        .finally(() => { this.detailLoading = false })
    },
    cancel(row) {
      this.$confirm(`确定取消「${row.patientName}」的预约吗？号源将回滚，余额支付会原路退款。`, '取消预约', { type: 'warning' })
        .then(() => appointmentApi.cancel(row.id))
        .then(() => { this.$message.success('预约已取消'); this.loadData() })
        .catch(() => {})
    },
    checkIn(row) {
      appointmentApi.checkIn(row.id)
        .then(() => { this.$message.success('签到成功'); this.loadData() })
        .catch(() => {})
    },
    changeStatus(row, target) {
      this.$confirm(`将状态从「${row.statusLabel}」流转为「${this.statusLabel(target)}」？`, '状态流转', { type: 'warning' })
        .then(() => appointmentApi.status(row.id, target))
        .then(() => { this.$message.success('状态已更新'); this.loadData() })
        .catch(() => {})
    },
    exportCsv() {
      this.exporting = true
      appointmentApi.exportCsv(this.pageParams())
        .then(blob => {
          const url = URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8' }))
          const link = document.createElement('a')
          link.href = url
          link.download = 'appointments_' + new Date().toISOString().slice(0, 10) + '.csv'
          link.click()
          URL.revokeObjectURL(url)
          this.$message.success('导出完成')
        })
        .catch(() => {})
        .finally(() => { this.exporting = false })
    }
  }
}
</script>
