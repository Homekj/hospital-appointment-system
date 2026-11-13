<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">就诊列表</h2>
        <p class="page-desc">我的患者预约（分页来自 /api/doctor/appointments），点击「接诊」进入病历与开方工作台</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.patientName" placeholder="就诊人姓名" clearable style="width: 160px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 150px">
            <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-date-picker v-model="query.startDate" type="date" value-format="yyyy-MM-dd" placeholder="就诊开始" style="width: 155px" />
          <el-date-picker v-model="query.endDate" type="date" value-format="yyyy-MM-dd" placeholder="就诊结束" style="width: 155px" />
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
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
        <el-table-column prop="departmentName" label="科室" min-width="110" />
        <el-table-column label="就诊日期" width="130">
          <template slot-scope="{ row }">{{ row.scheduleDate | date }}</template>
        </el-table-column>
        <el-table-column prop="timeSlot" label="时段" width="85" align="center" />
        <el-table-column label="挂号费" width="100" align="right">
          <template slot-scope="{ row }">{{ row.amount | money }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" effect="plain" :type="statusTagType(row.status)">{{ row.status | statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="showDetail(row)">详情</el-button>
            <el-button type="text" icon="el-icon-edit-outline" @click="openConsultation(row)">接诊</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无就诊记录</template>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :current-page="current" :page-size="size" :page-sizes="[10, 20, 50, 100]" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </div>

    <el-dialog title="预约详情" :visible.sync="detailVisible" width="600px">
      <div v-loading="detailLoading" class="detail-list">
        <el-row :gutter="12">
          <el-col :span="12"><p><strong>预约编号</strong>{{ detail.appointment && detail.appointment.id }}</p></el-col>
          <el-col :span="12"><p><strong>就诊人</strong>{{ detail.appointment && detail.appointment.patientName }}</p></el-col>
          <el-col :span="12"><p><strong>联系电话</strong>{{ (detail.appointment && detail.appointment.patientPhone) | maskPhone }}</p></el-col>
          <el-col :span="12"><p><strong>科室</strong>{{ detail.appointment && detail.appointment.departmentName }}</p></el-col>
          <el-col :span="12"><p><strong>就诊日期</strong>{{ (detail.appointment && detail.appointment.scheduleDate) | date }}</p></el-col>
          <el-col :span="12"><p><strong>时段</strong>{{ detail.appointment && detail.appointment.timeSlot }}</p></el-col>
          <el-col :span="12"><p><strong>状态</strong>{{ (detail.appointment && detail.appointment.status) | statusText }}</p></el-col>
          <el-col :span="12"><p><strong>挂号费</strong>{{ (detail.appointment && detail.appointment.amount) | money }}</p></el-col>
          <el-col :span="24"><p><strong>备注</strong>{{ (detail.appointment && detail.appointment.remark) || '-' }}</p></el-col>
          <el-col :span="24"><p><strong>病历</strong>{{ detail.consultation ? '已填写（' + (detail.consultation.diagnosis || '-') + '）' : '未填写' }}</p></el-col>
          <el-col :span="24"><p><strong>处方</strong>{{ detail.prescription ? '已开具，金额 ' + money(detail.prescription.totalAmount) : '未开具' }}</p></el-col>
        </el-row>
      </div>
      <span slot="footer"><el-button type="primary" @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { doctorApi } from '@/api/doctor'
import { STATUS_OPTIONS, statusTagType } from '@/utils/format'

export default {
  name: 'DoctorAppointments',
  mixins: [listPage],
  data() {
    return {
      STATUS_OPTIONS,
      detailVisible: false,
      detailLoading: false,
      detail: {}
    }
  },
  methods: {
    statusTagType,
    money(value) { return '¥' + Number(value || 0).toFixed(2) },
    defaultQuery() { return { patientName: '', status: '', startDate: '', endDate: '' } },
    fetchList(params) { return doctorApi.appointments(params) },
    saveData() { return Promise.resolve() },
    openConsultation(row) {
      this.$router.push({ path: '/doctor/consultation', query: { id: row.id } })
    },
    showDetail(row) {
      this.detail = {}
      this.detailVisible = true
      this.detailLoading = true
      doctorApi.detail(row.id)
        .then(data => { this.detail = data || {} })
        .catch(() => {})
        .finally(() => { this.detailLoading = false })
    }
  }
}
</script>
