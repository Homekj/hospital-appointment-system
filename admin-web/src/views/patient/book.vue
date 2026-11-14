<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">预约挂号</h2>
        <p class="page-desc">选科室 → 选医生 → 选号源 → 填写就诊人 → 支付挂号费 ¥20.00（后端固定）</p>
      </div>
    </div>

    <div class="panel">
      <el-steps :active="step" finish-status="success" align-center>
        <el-step title="选择科室与医生" />
        <el-step title="选择就诊号源" />
        <el-step title="填写就诊人信息" />
        <el-step title="支付挂号费" />
      </el-steps>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <div class="panel">
          <h3 class="block-title">1. 选择科室与医生</h3>
          <el-form label-width="90px" size="small">
            <el-form-item label="就诊科室">
              <el-select v-model="departmentId" filterable placeholder="请选择科室" style="width:100%" @change="onDepartmentChange">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="就诊医生">
              <el-select v-model="doctorId" filterable placeholder="请选择医生" style="width:100%" :disabled="!departmentId" @change="loadSchedules">
                <el-option v-for="d in doctors" :key="d.id" :label="`${d.name} · ${d.title || '医师'}`" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="就诊日期">
              <el-date-picker v-model="scheduleDate" type="date" value-format="yyyy-MM-dd" :picker-options="dateOptions" placeholder="选择日期" style="width:100%" @change="loadSchedules" />
            </el-form-item>
          </el-form>
          <div v-if="selectedDoctor" class="doctor-card">
            <strong>{{ selectedDoctor.name }}</strong>
            <span class="muted">{{ selectedDoctor.title }} · {{ selectedDoctor.departmentName }}</span>
            <p style="margin:8px 0 0;font-size:13px;color:#5b6577">{{ selectedDoctor.intro || '暂无简介' }}</p>
          </div>
        </div>
      </el-col>

      <el-col :span="14">
        <div class="panel">
          <h3 class="block-title">2. 选择就诊号源 <span class="muted" style="font-size:12px;font-weight:400">（仅显示可预约时段）</span></h3>
          <div v-loading="loadingSchedules">
            <el-radio-group v-model="scheduleId" style="width:100%">
              <el-radio v-for="s in availableSchedules" :key="s.id" :label="s.id" border class="slot-radio">
                {{ s.timeSlot }}　<span class="muted">剩余 {{ s.remainQuota }} / {{ s.totalQuota }}</span>
              </el-radio>
            </el-radio-group>
            <el-empty v-if="!availableSchedules.length" description="该医生当日暂无可预约号源，换个日期试试" :image-size="70" />
          </div>

          <h3 class="block-title" style="margin-top:22px">3. 填写就诊人信息</h3>
          <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
            <el-form-item label="就诊人" prop="patientName">
              <el-input v-model="form.patientName" maxlength="50" placeholder="真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="patientPhone">
              <el-input v-model="form.patientPhone" maxlength="11" placeholder="11 位手机号" />
            </el-form-item>
            <el-form-item label="身份证" prop="idCard">
              <el-input v-model="form.idCard" maxlength="30" placeholder="选填，仅用于就诊登记" />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" maxlength="200" placeholder="选填" />
            </el-form-item>
          </el-form>
          <el-button type="primary" icon="el-icon-circle-check" :loading="submitting" @click="submit">提交预约</el-button>
        </div>
      </el-col>
    </el-row>

    <el-dialog title="支付挂号费" :visible.sync="payVisible" width="420px" :close-on-click-modal="false" :show-close="false">
      <div v-if="created">
        <p style="font-size:14px;color:#4b5563;margin:0 0 14px">
          预约 <strong>#{{ created.id }}</strong> 已创建，当前状态为「待支付」。<br />
          应付金额：<strong style="color:#f56c6c;font-size:18px">{{ created.amount | money }}</strong>
        </p>
        <el-radio-group v-model="payMethod">
          <el-radio v-for="m in PAY_METHODS" :key="m.value" :label="m.value" style="display:block;margin:8px 0">{{ m.label }}</el-radio>
        </el-radio-group>
        <p class="muted" style="font-size:12px;margin-top:10px">余额支付会真实扣减账户余额；微信/支付宝为课程演示的模拟支付。</p>
      </div>
      <span slot="footer">
        <el-button @click="skipPay">稍后支付</el-button>
        <el-button type="primary" :loading="paying" @click="pay">确认支付</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { patientApi } from '@/api/patient'
import { PAY_METHODS } from '@/utils/format'

function tomorrow() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default {
  name: 'PatientBook',
  data() {
    return {
      PAY_METHODS,
      departments: [],
      doctors: [],
      schedules: [],
      departmentId: '',
      doctorId: '',
      scheduleDate: tomorrow(),
      scheduleId: '',
      loadingSchedules: false,
      form: { patientName: '', patientPhone: '', idCard: '', remark: '' },
      rules: {
        patientName: [{ required: true, message: '请填写就诊人姓名', trigger: 'blur' }],
        patientPhone: [
          { required: true, message: '请填写手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      },
      dateOptions: {
        disabledDate(date) { return date.getTime() < Date.now() - 86400000 }
      },
      submitting: false,
      payVisible: false,
      paying: false,
      payMethod: 'wechat',
      created: null
    }
  },
  computed: {
    selectedDoctor() { return this.doctors.find(d => d.id === this.doctorId) || null },
    availableSchedules() {
      return this.schedules.filter(s => s.status === 1 && Number(s.remainQuota) > 0)
    },
    step() {
      if (!this.departmentId || !this.doctorId) return 0
      if (!this.scheduleId) return 1
      if (!this.created) return 2
      return 3
    }
  },
  created() {
    patientApi.departments().then(list => { this.departments = list || [] }).catch(() => {})
  },
  methods: {
    onDepartmentChange() {
      this.doctorId = ''
      this.scheduleId = ''
      this.doctors = []
      this.schedules = []
      if (!this.departmentId) return
      patientApi.doctors({ departmentId: this.departmentId })
        .then(list => { this.doctors = list || [] })
        .catch(() => {})
    },
    loadSchedules() {
      this.scheduleId = ''
      this.schedules = []
      if (!this.doctorId || !this.scheduleDate) return
      this.loadingSchedules = true
      patientApi.schedules({ doctorId: this.doctorId, scheduleDate: this.scheduleDate })
        .then(list => { this.schedules = list || [] })
        .catch(() => { this.schedules = [] })
        .finally(() => { this.loadingSchedules = false })
    },
    submit() {
      let valid = false
      this.$refs.form.validate(v => { valid = v })
      if (!valid) return
      if (!this.scheduleId) { this.$message.warning('请先选择就诊号源'); return }
      this.submitting = true
      patientApi.createAppointment(Object.assign({ scheduleId: this.scheduleId }, this.form))
        .then(data => {
          this.created = data
          this.payVisible = true
        })
        .catch(() => {})
        .finally(() => { this.submitting = false })
    },
    pay() {
      if (!this.created) return
      this.paying = true
      patientApi.payAppointment(this.created.id, this.payMethod)
        .then(() => {
          this.$message.success('支付成功，预约已生效')
          this.payVisible = false
          this.$router.push('/patient/appointments')
        })
        .catch(() => {})
        .finally(() => { this.paying = false })
    },
    skipPay() {
      this.payVisible = false
      this.$message.info('预约已创建，可在「我的预约」中继续支付')
      this.$router.push('/patient/appointments')
    }
  }
}
</script>

<style scoped>
.block-title { font-size: 16px; margin: 0 0 16px; }
.slot-radio { margin: 0 10px 10px 0 !important; min-width: 190px; }
.doctor-card { border-top: 1px dashed #e9edf4; padding-top: 12px; font-size: 14px; }
.doctor-card span { margin-left: 10px; font-size: 13px; }
</style>
