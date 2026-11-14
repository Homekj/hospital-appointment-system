<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="page-title">数据概览</h2>
        <p class="page-desc">实时统计来自 /api/admin/dashboard/stats，所有数字直接取自业务库</p>
      </div>
      <el-button icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日预约</div>
        <div class="stat-value">{{ stats.todayAppointments || 0 }}</div>
        <div class="stat-hint">按预约创建时间统计</div>
        <i class="el-icon-date stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">今日收入</div>
        <div class="stat-value">{{ (stats.todayRevenue || 0) | money }}</div>
        <div class="stat-hint">已支付且未取消的挂号费</div>
        <i class="el-icon-money stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">待处理预约</div>
        <div class="stat-value">{{ stats.pendingAppointments || 0 }}</div>
        <div class="stat-hint">待支付 / 待就诊 / 已签到</div>
        <i class="el-icon-time stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成就诊</div>
        <div class="stat-value">{{ stats.completedAppointments || 0 }}</div>
        <div class="stat-hint">累计完成</div>
        <i class="el-icon-circle-check stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">启用医生 / 科室</div>
        <div class="stat-value">{{ stats.doctorCount || 0 }} / {{ stats.departmentCount || 0 }}</div>
        <div class="stat-hint">仅统计启用状态</div>
        <i class="el-icon-user stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">系统用户</div>
        <div class="stat-value">{{ stats.userCount || 0 }}</div>
        <div class="stat-hint">含管理员、医生、患者</div>
        <i class="el-icon-s-custom stat-icon" />
      </div>
    </div>

    <div class="panel">
      <div class="page-head" style="margin-bottom: 8px">
        <div>
          <h3 style="margin: 0; font-size: 16px">近 7 日预约量</h3>
          <p class="page-desc">柱状高度为当日预约单数，悬停可查看当日收入</p>
        </div>
        <el-tag type="info" size="small" effect="plain">最近 7 天</el-tag>
      </div>
      <div class="chart-wrap" v-if="trend.length">
        <div class="chart-col" v-for="item in trend" :key="item.date">
          <div class="chart-value">{{ item.appointmentCount || 0 }}</div>
          <el-tooltip placement="top" :content="`${item.date} · 收入 ${formatMoney(item.revenue)}`">
            <div class="chart-bar" :style="{ height: barHeight(item.appointmentCount) }" />
          </el-tooltip>
          <div class="chart-label">{{ item.date.slice(5) }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无趋势数据" :image-size="70" />
    </div>

    <div class="panel">
      <h3 style="margin: 0 0 12px; font-size: 16px">快捷入口</h3>
      <el-button icon="el-icon-office-building" @click="$router.push('/admin/departments')">科室管理</el-button>
      <el-button icon="el-icon-user" @click="$router.push('/admin/doctors')">医生管理</el-button>
      <el-button icon="el-icon-date" @click="$router.push('/admin/schedules')">排班管理</el-button>
      <el-button icon="el-icon-tickets" @click="$router.push('/admin/appointments')">预约管理</el-button>
    </div>
  </div>
</template>

<script>
import { dashboardApi } from '@/api/admin'

export default {
  name: 'DashboardPage',
  data() {
    return {
      loading: false,
      stats: {},
      trend: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      dashboardApi.stats()
        .then(data => {
          this.stats = data || {}
          this.trend = (data && data.last7Days) || []
        })
        .catch(() => {})
        .finally(() => { this.loading = false })
    },
    barHeight(value) {
      const max = Math.max(1, ...this.trend.map(item => Number(item.appointmentCount || 0)))
      return Math.max(6, Math.round((Number(value || 0) / max) * 120)) + 'px'
    },
    formatMoney(value) {
      return '¥' + Number(value || 0).toFixed(2)
    }
  }
}
</script>
