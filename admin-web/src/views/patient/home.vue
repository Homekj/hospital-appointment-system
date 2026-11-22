<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的首页</h2>
        <p class="page-desc">欢迎回来，{{ user.nickname || user.username || '患者' }}　|　账号数据来自 /api/auth/me</p>
      </div>
      <el-button icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">账户余额</div>
        <div class="stat-value">{{ user.balance | money }}</div>
        <div class="stat-hint">可用于挂号费与药费支付</div>
        <i class="el-icon-wallet stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">累计预约</div>
        <div class="stat-value">{{ total }}</div>
        <div class="stat-hint">全部挂号记录</div>
        <i class="el-icon-tickets stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">待就诊</div>
        <div class="stat-value">{{ upcoming }}</div>
        <div class="stat-hint">待支付 / 待就诊 / 已到院</div>
        <i class="el-icon-time stat-icon" />
      </div>
      <div class="stat-card">
        <div class="stat-label">未读消息</div>
        <div class="stat-value">{{ unread }}</div>
        <div class="stat-hint">站内通知</div>
        <i class="el-icon-bell stat-icon" />
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="panel">
          <div class="page-head" style="margin-bottom:12px">
            <div><h3 style="margin:0;font-size:16px">最近预约</h3><p class="page-desc">最新 5 条挂号记录</p></div>
            <el-button type="text" @click="$router.push('/patient/appointments')">查看全部</el-button>
          </div>
          <el-table :data="recent" size="small" border stripe>
            <el-table-column prop="doctorName" label="医生" width="110" />
            <el-table-column prop="departmentName" label="科室" min-width="110" />
            <el-table-column label="就诊日期" width="120">
              <template slot-scope="{ row }">{{ row.scheduleDate | date }}</template>
            </el-table-column>
            <el-table-column prop="timeSlot" label="时段" width="80" align="center" />
            <el-table-column label="状态" width="110" align="center">
              <template slot-scope="{ row }">
                <el-tag size="mini" effect="plain" :type="statusTagType(row.status)">{{ row.status | statusText }}</el-tag>
              </template>
            </el-table-column>
            <template slot="empty">暂无预约记录，去挂个号吧</template>
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="panel">
          <h3 style="margin:0 0 6px;font-size:16px">就医导航</h3>
          <p class="page-desc" style="margin-bottom:12px">描述症状，查看可能相关的科室。出现紧急情况，请立即寻求线下急救。</p>
          <el-input v-model="symptom" type="textarea" :rows="2" maxlength="200" placeholder="例如：头痛、发热、咳嗽三天" />
          <el-button type="primary" size="small" style="margin-top:10px" :loading="triaging" @click="analyze">推荐科室</el-button>
          <div v-if="triage" class="triage-result">
            <el-alert :type="triage.urgent || triage.requiresHumanHelp ? 'warning' : 'info'" :closable="false" show-icon :title="triage.preliminaryConclusion || triage.summary" />
            <p style="font-size:13px;color:#5b6577;margin:10px 0 6px">{{ triage.advice }}</p>
            <el-tag v-for="d in triage.departments" :key="d.id" size="small" effect="plain" style="margin:0 6px 6px 0">{{ d.name }}</el-tag>
            <a v-if="triage.urgent" href="tel:120">拨打 120</a>
            <a v-else-if="triage.helpPhone" :href="'tel:' + triage.helpPhone.replace(/[^0-9+]/g, '')">联系导诊台 {{ triage.helpPhone }}</a>
            <p v-else-if="triage.requiresHumanHelp" class="muted">可到门诊服务台咨询。</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="panel">
      <h3 style="margin:0 0 12px;font-size:16px">快捷入口</h3>
      <el-button type="primary" icon="el-icon-circle-plus" @click="$router.push('/patient/book')">预约挂号</el-button>
      <el-button icon="el-icon-tickets" @click="$router.push('/patient/appointments')">我的预约</el-button>
      <el-button icon="el-icon-document" @click="$router.push('/patient/clinical')">病历与处方</el-button>
      <el-button icon="el-icon-bell" @click="$router.push('/patient/notifications')">消息通知</el-button>
      <el-button icon="el-icon-wallet" @click="$router.push('/patient/wallet')">余额充值</el-button>
    </div>
  </div>
</template>

<script>
import { authApi } from '@/api/admin'
import { patientApi } from '@/api/patient'
import { statusTagType } from '@/utils/format'
import { setAuth, getToken } from '@/utils/auth'

export default {
  name: 'PatientHome',
  data() {
    return {
      loading: false,
      user: {},
      recent: [],
      total: 0,
      upcoming: 0,
      unread: 0,
      symptom: '',
      triaging: false,
      triage: null
    }
  },
  created() { this.load() },
  methods: {
    statusTagType,
    load() {
      this.loading = true
      Promise.all([
        authApi.me(),
        patientApi.appointments({ current: 1, size: 5 }),
        patientApi.appointments({ current: 1, size: 1, status: 'pending' }),
        patientApi.unreadCount()
      ]).then(([user, recent, pending, unread]) => {
        this.user = user || {}
        // 同步 localStorage，保证刷新后余额等字段是最新的
        if (user) setAuth({ token: getToken(), user })
        this.recent = (recent && recent.records) || []
        this.total = Number((recent && recent.total) || 0)
        this.upcoming = Number((pending && pending.total) || 0)
        this.unread = Number(unread || 0)
      }).catch(() => {}).finally(() => { this.loading = false })
    },
    analyze() {
      if (!this.symptom.trim()) { this.$message.warning('请先描述症状'); return }
      this.triaging = true
      patientApi.triage(this.symptom.trim())
        .then(data => { this.triage = data || null })
        .catch(() => { this.triage = null })
        .finally(() => { this.triaging = false })
    }
  }
}
</script>

<style scoped>
.triage-result { margin-top: 14px; }
</style>
