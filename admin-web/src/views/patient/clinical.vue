<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">病历与处方</h2>
        <p class="page-desc">查看就诊病历、处方明细，并完成处方支付与取药</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-select v-model="appointmentId" filterable clearable placeholder="选择有就诊记录的预约" style="width: 380px" @change="load">
            <el-option v-for="a in candidates" :key="a.id" :label="`#${a.id} ${a.doctorName} · ${formatDay(a.scheduleDate)} · ${statusText(a.status)}`" :value="a.id" />
          </el-select>
          <el-input v-model="manualId" placeholder="或输入预约 ID" style="width: 170px" @keyup.enter.native="loadById" />
          <el-button type="primary" icon="el-icon-search" @click="loadById">查询</el-button>
        </div>
        <el-button type="text" @click="$router.push('/patient/appointments')">我的预约 →</el-button>
      </div>
    </div>

    <div v-if="!detail.id && !loading" class="panel">
      <el-empty description="请选择一个预约查看病历与处方" :image-size="80" />
    </div>

    <div v-if="detail.id" v-loading="loading">
      <div class="panel">
        <div class="page-head" style="margin-bottom:6px">
          <div>
            <h3 style="margin:0;font-size:16px">就诊 #{{ detail.id }} · {{ detail.doctorName }} {{ detail.doctorTitle || '' }}</h3>
            <p class="page-desc">{{ detail.departmentName }} · {{ formatDay(detail.scheduleDate) }} {{ detail.timeSlot }} · {{ detail.patientName }}</p>
          </div>
          <el-tag effect="plain" :type="statusTagType(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :span="10">
          <div class="panel">
            <h3 style="margin:0 0 14px;font-size:16px">病历记录</h3>
            <div v-if="consultation" class="detail-list">
              <p><strong>主诉</strong>{{ consultation.chiefComplaint || '-' }}</p>
              <p><strong>现病史</strong>{{ consultation.presentIllness || '-' }}</p>
              <p><strong>诊断</strong>{{ consultation.diagnosis || '-' }}</p>
              <p><strong>治疗方案</strong>{{ consultation.treatmentPlan || '-' }}</p>
              <p><strong>医嘱</strong>{{ consultation.doctorAdvice || '-' }}</p>
              <p><strong>记录时间</strong>{{ consultation.createTime | time }}</p>
            </div>
            <el-empty v-else description="本次就诊暂无病历记录" :image-size="70" />
          </div>
        </el-col>
        <el-col :span="14">
          <div class="panel">
            <div class="page-head" style="margin-bottom:12px">
              <div><h3 style="margin:0;font-size:16px">处方明细</h3><p class="page-desc">金额为服务端按药品单价计算</p></div>
              <el-tag v-if="prescription" effect="plain" :type="prescriptionTagType(prescription.status)">
                {{ prescriptionStatusText(prescription.status) }}
              </el-tag>
            </div>
            <template v-if="prescription">
              <el-table :data="prescription.items || []" border size="small">
                <el-table-column prop="medicineName" label="药品" min-width="130" />
                <el-table-column prop="spec" label="规格" min-width="100" />
                <el-table-column prop="dosage" label="用法" min-width="90" />
                <el-table-column prop="frequency" label="频次" min-width="90" />
                <el-table-column prop="quantity" label="数量" width="70" align="center" />
                <el-table-column label="单价" width="95" align="right">
                  <template slot-scope="{ row }">{{ row.unitPrice | money }}</template>
                </el-table-column>
                <el-table-column label="小计" width="95" align="right">
                  <template slot-scope="{ row }">{{ row.amount | money }}</template>
                </el-table-column>
                <template slot="empty">处方无明细</template>
              </el-table>
              <div style="margin-top:14px;display:flex;align-items:center;justify-content:space-between">
                <strong style="font-size:16px">合计：<span style="color:#f56c6c">{{ prescription.totalAmount | money }}</span></strong>
                <div v-if="prescription.status === 'unpaid'">
                  <el-select v-model="payMethod" size="small" style="width:150px">
                    <el-option v-for="m in PAY_METHODS" :key="m.value" :label="m.label" :value="m.value" />
                  </el-select>
                  <el-button type="primary" size="small" :loading="paying" style="margin-left:8px" @click="pay">支付药费</el-button>
                </div>
                <div v-else-if="prescription.status === 'paid'">
                  <span class="muted">{{ prescription.payMethodText || '已支付' }}</span>
                  <el-button type="success" size="small" :loading="pickingUp" style="margin-left:10px" @click="pickup">确认已取药</el-button>
                </div>
                <span v-else class="muted">{{ prescription.payMethodText || prescriptionStatusText(prescription.status) }}</span>
              </div>
              <p class="muted" style="font-size:12px;margin-top:10px">确认取药后，处方会标记为已取药，预约状态同步更新为已完成。</p>
            </template>
            <el-empty v-else description="本次就诊未开具处方" :image-size="70" />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { patientApi } from '@/api/patient'
import { PAY_METHODS, statusText, statusTagType, formatDate } from '@/utils/format'

export default {
  name: 'PatientClinical',
  data() {
    return {
      PAY_METHODS,
      appointmentId: '',
      manualId: '',
      candidates: [],
      detail: {},
      consultation: null,
      prescription: null,
      loading: false,
      paying: false,
      pickingUp: false,
      payMethod: 'wechat'
    }
  },
  created() {
    this.loadCandidates()
    const id = this.$route.query.appointmentId
    if (id) {
      this.appointmentId = Number(id)
      this.load()
    }
  },
  methods: {
    statusText, statusTagType,
    formatDay: formatDate,
    prescriptionStatusText(status) {
      return ({ unpaid: '待支付', paid: '待取药', dispensed: '已取药', cancelled: '已取消' })[status] || status || '-'
    },
    prescriptionTagType(status) {
      return ({ unpaid: 'warning', paid: 'primary', dispensed: 'success', cancelled: 'info' })[status] || 'info'
    },
    loadCandidates() {
      Promise.all([
        patientApi.appointments({ current: 1, size: 30, status: 'completed' }),
        patientApi.appointments({ current: 1, size: 30, status: 'pending_dispense' })
      ]).then(([done, rx]) => {
        this.candidates = [...((done && done.records) || []), ...((rx && rx.records) || [])]
      }).catch(() => {})
    },
    loadById() {
      const id = Number(this.manualId)
      if (!id) { this.$message.warning('请输入预约 ID'); return }
      this.appointmentId = id
      this.load()
    },
    load() {
      const id = this.appointmentId
      if (!id) { this.detail = {}; this.consultation = null; this.prescription = null; return }
      this.loading = true
      this.consultation = null
      this.prescription = null
      patientApi.appointmentDetail(id)
        .then(data => {
          this.detail = data || {}
          const tasks = []
          if (data && data.hasConsultation) {
            tasks.push(patientApi.consultation(id).then(c => { this.consultation = c }).catch(() => {}))
          }
          if (data && data.prescriptionId) {
            tasks.push(patientApi.prescriptionByAppointment(id)
              .then(p => { this.prescription = p })
              .catch(() => {}))
          }
          return Promise.all(tasks)
        })
        .catch(() => { this.detail = {} })
        .finally(() => { this.loading = false })
    },
    pay() {
      if (!this.prescription) return
      this.paying = true
      patientApi.payPrescription(this.prescription.id, this.payMethod)
        .then(() => { this.$message.success('药费支付成功'); this.load() })
        .catch(() => {})
        .finally(() => { this.paying = false })
    },
    pickup() {
      if (!this.prescription || this.prescription.status !== 'paid') return
      this.$confirm('确认已经从药房领取处方中的药品？确认后本次就诊将标记为已完成。', '确认取药', { type: 'warning' })
        .then(() => {
          this.pickingUp = true
          return patientApi.pickupPrescription(this.prescription.id)
        })
        .then(() => {
          this.$message.success('取药已确认，就诊已完成')
          this.load()
          this.loadCandidates()
        })
        .catch(() => {})
        .finally(() => { this.pickingUp = false })
    }
  }
}
</script>
