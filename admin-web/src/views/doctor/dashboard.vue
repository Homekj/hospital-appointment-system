<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="page-title">工作台概览</h2>
        <p class="page-desc">{{ profile.name || '医生' }} {{ profile.title || '' }} · {{ profile.departmentName || '' }}　|　数据来自 /api/doctor/dashboard</p>
      </div>
      <el-button icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日就诊</div>
        <div class="stat-value">{{ stats.todayAppointments || 0 }}</div>
        <div class="stat-hint">按就诊日期统计</div>
        <i class="el-icon-date stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">待接诊</div>
        <div class="stat-value">{{ stats.waitingCount || 0 }}</div>
        <div class="stat-hint">待就诊 + 已到院</div>
        <i class="el-icon-time stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">就诊中</div>
        <div class="stat-value">{{ stats.inConsultationCount || 0 }}</div>
        <div class="stat-hint">需填写病历</div>
        <i class="el-icon-edit-outline stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">待缴药费</div>
        <div class="stat-value">{{ stats.pendingRxPayCount || 0 }}</div>
        <div class="stat-hint">已开处方待支付</div>
        <i class="el-icon-first-aid-kit stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value">{{ stats.completedCount || 0 }}</div>
        <div class="stat-hint">累计完成接诊 {{ stats.totalAppointments || 0 }} 单</div>
        <i class="el-icon-circle-check stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">今日号源</div>
        <div class="stat-value">{{ stats.todayRemainQuota || 0 }} / {{ stats.todaySchedules || 0 }}</div>
        <div class="stat-hint">剩余号源 / 排班场次</div>
        <i class="el-icon-s-grid stat-icon" />
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="panel">
          <div class="page-head" style="margin-bottom: 12px">
            <div><h3 style="margin:0; font-size:16px">待接诊患者</h3><p class="page-desc">已到院 / 待就诊，点击可直接进入接诊工作台</p></div>
            <el-button type="text" @click="$router.push('/doctor/appointments')">查看全部</el-button>
          </div>
          <el-table :data="waiting" size="small" border stripe>
            <el-table-column prop="timeSlot" label="时段" width="80" align="center" />
            <el-table-column prop="patientName" label="就诊人" width="110" />
            <el-table-column prop="departmentName" label="科室" min-width="110" />
            <el-table-column prop="scheduleDate" label="就诊日期" width="120">
              <template slot-scope="{ row }">{{ row.scheduleDate | date }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="{ row }">
                <el-tag size="mini" effect="plain" :type="statusTagType(row.status)">{{ row.status | statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" :fixed="$root.isMobile ? false : 'right'">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openConsultation(row)">接诊</el-button>
              </template>
            </el-table-column>
            <template slot="empty">暂无待接诊患者</template>
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="panel">
          <div class="page-head" style="margin-bottom: 12px">
            <div><h3 style="margin:0; font-size:16px">今日排班</h3><p class="page-desc">{{ today }}</p></div>
            <el-button type="text" @click="$router.push('/doctor/schedules')">我的排班</el-button>
          </div>
          <el-table :data="todaySchedules" size="small" border stripe>
            <el-table-column prop="timeSlot" label="时段" width="80" align="center" />
            <el-table-column prop="totalQuota" label="号源" width="70" align="center" />
            <el-table-column prop="bookedCount" label="已约" width="70" align="center" />
            <el-table-column prop="remainQuota" label="剩余" width="70" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template slot-scope="{ row }">
                <el-tag size="mini" effect="plain" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停诊' }}</el-tag>
              </template>
            </el-table-column>
            <template slot="empty">今日无排班</template>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { doctorApi } from '@/api/doctor'
import { statusTagType } from '@/utils/format'

export default {
  name: 'DoctorDashboard',
  data() {
    return {
      today: new Date().toISOString().slice(0, 10),
      loading: false,
      stats: {},
      profile: {},
      waiting: [],
      todaySchedules: []
    }
  },
  created() { this.load() },
  methods: {
    statusTagType,
    load() {
      this.loading = true
      Promise.all([
        doctorApi.dashboard(),
        doctorApi.me(),
        doctorApi.appointments({ current: 1, size: 8, status: 'checked_in' }),
        doctorApi.schedules({ startDate: this.today, endDate: this.today })
      ]).then(([stats, profile, page, schedules]) => {
        this.stats = stats || {}
        this.profile = profile || {}
        this.waiting = (page && page.records) || []
        this.todaySchedules = schedules || []
      }).catch(() => {}).finally(() => { this.loading = false })
    },
    openConsultation(row) {
      this.$router.push({ path: '/doctor/consultation', query: { id: row.id } })
    }
  }
}
</script>
