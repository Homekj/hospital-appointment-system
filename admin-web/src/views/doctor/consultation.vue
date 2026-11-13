<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">接诊工作台</h2>
        <p class="page-desc">叫号 → 填写病历 → 完成接诊（可同时开处方），全部走 /api/doctor/appointments 状态机</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-select v-model="appointmentId" filterable clearable placeholder="选择待接诊 / 就诊中的预约" style="width: 380px" @change="loadDetail">
            <el-option v-for="a in candidates" :key="a.id" :label="`#${a.id} ${a.patientName} · ${formatDay(a.scheduleDate)} ${a.timeSlot} · ${statusText(a.status)}`" :value="a.id" />
          </el-select>
          <el-input v-model="manualId" placeholder="或直接输入预约 ID" style="width: 180px" @keyup.enter.native="loadById" />
          <el-button type="primary" icon="el-icon-search" @click="loadById">加载</el-button>
          <el-button icon="el-icon-refresh" @click="loadCandidates">刷新列表</el-button>
        </div>
      </div>
    </div>

    <div v-if="!detail.appointment && !loading" class="panel">
      <el-empty description="请先选择或输入一个预约 ID" :image-size="80" />
    </div>

    <div v-if="detail.appointment" v-loading="loading">
      <div class="panel">
        <div class="page-head" style="margin-bottom: 8px">
          <div>
            <h3 style="margin:0; font-size:16px">预约 #{{ detail.appointment.id }} · {{ detail.appointment.patientName }}</h3>
            <p class="page-desc">
              {{ detail.appointment.departmentName }} · {{ formatDay(detail.appointment.scheduleDate) }} {{ detail.appointment.timeSlot }} ·
              电话 {{ maskPhone(detail.appointment.patientPhone) }} · 挂号费 {{ money(detail.appointment.amount) }}
            </p>
          </div>
          <el-tag :type="statusTagType(detail.appointment.status)" effect="plain">{{ statusText(detail.appointment.status) }}</el-tag>
        </div>
        <el-steps :active="stepActive" finish-status="success" simple>
          <el-step title="已预约" />
          <el-step title="到院签到" />
          <el-step title="开始接诊 / 写病历" />
          <el-step title="完成接诊 / 开方" />
        </el-steps>
        <div style="margin-top: 16px">
          <el-button v-if="canStart" type="primary" icon="el-icon-video-play" :loading="starting" @click="start">开始接诊（叫号）</el-button>
          <el-tag v-if="!canStart && !canSaveClinical" type="info" effect="plain">{{ statusHint }}</el-tag>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="panel">
            <h3 style="margin:0 0 14px; font-size:16px">病历记录</h3>
            <el-form ref="clinicalForm" :model="clinicalForm" :rules="clinicalRules" label-width="90px" size="small">
              <el-form-item label="主诉" prop="chiefComplaint">
                <el-input v-model="clinicalForm.chiefComplaint" type="textarea" :rows="2" maxlength="500" :disabled="!canSaveClinical" placeholder="患者主要症状" />
              </el-form-item>
              <el-form-item label="现病史">
                <el-input v-model="clinicalForm.presentIllness" type="textarea" :rows="2" maxlength="1000" :disabled="!canSaveClinical" />
              </el-form-item>
              <el-form-item label="诊断" prop="diagnosis">
                <el-input v-model="clinicalForm.diagnosis" type="textarea" :rows="2" maxlength="500" :disabled="!canSaveClinical" />
              </el-form-item>
              <el-form-item label="治疗方案">
                <el-input v-model="clinicalForm.treatmentPlan" type="textarea" :rows="2" maxlength="1000" :disabled="!canSaveClinical" />
              </el-form-item>
              <el-form-item label="医嘱">
                <el-input v-model="clinicalForm.doctorAdvice" type="textarea" :rows="2" maxlength="500" :disabled="!canSaveClinical" />
              </el-form-item>
            </el-form>
            <el-button type="primary" size="small" icon="el-icon-document-checked" :disabled="!canSaveClinical" :loading="saving" @click="saveClinical">保存病历</el-button>
            <span v-if="!canSaveClinical" class="muted" style="margin-left:10px; font-size:12px">仅「就诊中」状态可填写病历</span>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="panel">
            <h3 style="margin:0 0 8px; font-size:16px">处方与完成接诊</h3>
            <p class="page-desc" style="margin-bottom:12px">不添加药品即为「无处方完成」；单价由后端按药品表计算，前端不传价格。</p>

            <div v-if="detail.prescription" class="prescription-done">
              <el-alert type="success" :closable="false" show-icon
                :title="`已开处方 #${detail.prescription.id}，金额 ${money(detail.prescription.totalAmount)}，状态 ${detail.prescription.status}`" />
            </div>
            <template v-else>
              <div class="toolbar" style="margin-bottom:10px">
                <span class="muted" style="font-size:13px">药品明细（{{ items.length }} 项）</span>
                <el-button size="mini" icon="el-icon-plus" :disabled="!canComplete" @click="addItem">添加药品</el-button>
              </div>
              <el-table :data="items" size="mini" border>
                <el-table-column label="药品" min-width="180">
                  <template slot-scope="{ row }">
                    <el-select v-model="row.medicineId" filterable size="mini" placeholder="选择药品" style="width:100%" :disabled="!canComplete">
                      <el-option v-for="m in medicineOptions" :key="m.id" :label="`${m.name} ${m.spec || ''} ¥${m.price}`" :value="m.id" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="90">
                  <template slot-scope="{ row }"><el-input-number v-model="row.quantity" :min="1" size="mini" controls-position="right" style="width:80px" :disabled="!canComplete" /></template>
                </el-table-column>
                <el-table-column label="用法" width="120">
                  <template slot-scope="{ row }"><el-input v-model="row.dosage" size="mini" placeholder="每次1片" :disabled="!canComplete" /></template>
                </el-table-column>
                <el-table-column label="频次" width="120">
                  <template slot-scope="{ row }"><el-input v-model="row.frequency" size="mini" placeholder="每日3次" :disabled="!canComplete" /></template>
                </el-table-column>
                <el-table-column label="天数" width="90">
                  <template slot-scope="{ row }"><el-input-number v-model="row.days" :min="1" size="mini" controls-position="right" style="width:80px" :disabled="!canComplete" /></template>
                </el-table-column>
                <el-table-column label="" width="60">
                  <template slot-scope="{ $index }"><el-button type="text" icon="el-icon-delete" class="danger-link" @click="items.splice($index, 1)" /></template>
                </el-table-column>
                <template slot="empty">暂无药品，直接完成即无处方</template>
              </el-table>
              <el-button type="success" icon="el-icon-circle-check" style="margin-top:14px" :disabled="!canComplete" :loading="completing" @click="complete">完成接诊{{ items.length ? '并开处方' : '' }}</el-button>
              <div v-if="!canComplete" class="muted" style="font-size:12px; margin-top:10px">需先「开始接诊」并保存病历，才能完成接诊。</div>
            </template>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { doctorApi } from '@/api/doctor'
import { statusText, statusTagType, formatDate } from '@/utils/format'

const emptyClinical = () => ({
  chiefComplaint: '', presentIllness: '', diagnosis: '', treatmentPlan: '', doctorAdvice: ''
})

export default {
  name: 'DoctorConsultation',
  data() {
    return {
      appointmentId: '',
      manualId: '',
      candidates: [],
      medicineOptions: [],
      detail: {},
      clinicalForm: emptyClinical(),
      items: [],
      loading: false,
      starting: false,
      saving: false,
      completing: false,
      clinicalRules: {
        chiefComplaint: [{ required: true, message: '请填写主诉', trigger: 'blur' }],
        diagnosis: [{ required: true, message: '请填写诊断', trigger: 'blur' }]
      }
    }
  },
  computed: {
    status() { return (this.detail.appointment && this.detail.appointment.status) || '' },
    canStart() { return this.status === 'checked_in' },
    canSaveClinical() { return this.status === 'in_consultation' },
    canComplete() { return this.status === 'in_consultation' && !!this.detail.consultation },
    statusHint() {
      return { unpaid: '患者尚未支付挂号费', pending: '患者尚未签到', completed: '该预约已完成', cancelled: '该预约已取消', pending_rx_pay: '已开处方，等待患者缴药费' }[this.status] || '当前状态不可操作'
    },
    stepActive() {
      return { pending: 1, checked_in: 2, in_consultation: 3, completed: 4, pending_rx_pay: 4 }[this.status] || 0
    }
  },
  created() {
    this.loadCandidates()
    doctorApi.medicines().then(list => { this.medicineOptions = list || [] }).catch(() => {})
    const id = this.$route.query.id
    if (id) {
      this.appointmentId = Number(id)
      this.loadDetail(this.appointmentId)
    }
  },
  methods: {
    statusText, statusTagType,
    formatDay: formatDate,
    money(value) { return '¥' + Number(value || 0).toFixed(2) },
    maskPhone(value) { return value ? String(value).slice(0, 3) + '****' + String(value).slice(-4) : '-' },
    loadCandidates() {
      Promise.all([
        doctorApi.appointments({ current: 1, size: 30, status: 'checked_in' }),
        doctorApi.appointments({ current: 1, size: 30, status: 'in_consultation' })
      ]).then(([checked, ongoing]) => {
        this.candidates = [...((checked && checked.records) || []), ...((ongoing && ongoing.records) || [])]
      }).catch(() => {})
    },
    loadById() {
      const id = Number(this.manualId)
      if (!id) { this.$message.warning('请输入预约 ID'); return }
      this.appointmentId = id
      this.loadDetail(id)
    },
    loadDetail(id) {
      if (!id) { this.detail = {}; return }
      this.loading = true
      doctorApi.detail(id)
        .then(data => {
          this.detail = data || {}
          this.clinicalForm = Object.assign(emptyClinical(), data && data.consultation ? data.consultation : {})
        })
        .catch(() => { this.detail = {} })
        .finally(() => { this.loading = false })
    },
    start() {
      this.starting = true
      doctorApi.start(this.appointmentId)
        .then(() => { this.$message.success('已叫号，开始接诊'); this.loadDetail(this.appointmentId); this.loadCandidates() })
        .catch(() => {})
        .finally(() => { this.starting = false })
    },
    saveClinical() {
      this.$refs.clinicalForm.validate(valid => {
        if (!valid) return
        this.saving = true
        doctorApi.saveClinical(this.appointmentId, this.clinicalForm)
          .then(() => { this.$message.success('病历已保存'); this.loadDetail(this.appointmentId) })
          .catch(() => {})
          .finally(() => { this.saving = false })
      })
    },
    addItem() {
      this.items.push({ medicineId: '', quantity: 1, dosage: '', frequency: '', days: 1 })
    },
    complete() {
      const invalid = this.items.some(item => !item.medicineId || !item.quantity)
      if (invalid) { this.$message.warning('请为每一项选择药品并填写数量'); return }
      this.completing = true
      doctorApi.complete(this.appointmentId, this.items)
        .then(result => {
          this.$message.success(result && result.prescriptionId ? '接诊完成，处方已开具（待缴药费）' : '接诊完成')
          this.items = []
          this.loadDetail(this.appointmentId)
          this.loadCandidates()
        })
        .catch(() => {})
        .finally(() => { this.completing = false })
    }
  }
}
</script>

<style scoped>
.prescription-done { margin-bottom: 12px; }
</style>
