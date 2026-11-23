<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">预约挂号</h2>
        <p class="page-desc">选科室 → 选医生 → 选号源 → 确认就诊人 → 支付挂号费</p>
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
                {{ s.timeSlot }}　{{ s.registrationFee | money }}　<span class="muted">剩余 {{ s.remainQuota }} / {{ s.totalQuota }}</span>
              </el-radio>
            </el-radio-group>
            <el-empty v-if="!availableSchedules.length" description="该医生当日暂无可预约号源，换个日期试试" :image-size="70" />
          </div>

          <h3 class="block-title" style="margin-top:22px">3. 填写就诊人信息</h3>
          <el-form label-width="90px" size="small">
            <el-form-item label="常用就诊人">
              <el-select v-model="patientProfileId" clearable placeholder="选择就诊人或手动填写">
                <el-option v-for="p in profiles" :key="p.id" :value="p.id"
                  :label="p.name + ' · ' + p.relationship + (p.defaultPatient ? '（默认）' : '')" />
              </el-select>
              <el-button type="text" @click="openProfiles">管理就诊人</el-button>
            </el-form-item>
          </el-form>
          <el-form ref="form" :model="form" :rules="rules" label-width="90px" size="small">
            <template v-if="!patientProfileId">
            <el-form-item label="就诊人" prop="patientName">
              <el-input v-model="form.patientName" maxlength="50" placeholder="真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="patientPhone">
              <el-input v-model="form.patientPhone" maxlength="11" placeholder="11 位手机号" />
            </el-form-item>
            <el-form-item label="身份证" prop="idCard">
              <el-input v-model="form.idCard" maxlength="30" placeholder="选填，仅用于就诊登记" />
            </el-form-item>
            </template>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" maxlength="200" placeholder="选填" />
            </el-form-item>
          </el-form>
          <el-button type="primary" icon="el-icon-circle-check" :loading="submitting" @click="submit">提交预约</el-button>
        </div>
      </el-col>
    </el-row>

    <el-dialog title="常用就诊人" :visible.sync="profilesVisible" width="520px">
      <el-table :data="profiles" empty-text="暂无常用就诊人">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="relationship" label="关系" width="75" />
        <el-table-column label="默认" width="65">
          <template slot-scope="{ row }">{{ row.defaultPatient ? '是' : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="125">
          <template slot-scope="{ row }">
            <el-button type="text" @click="editProfile(row)">编辑</el-button>
            <el-button type="text" :disabled="profileBusy" @click="removeProfile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-form :model="profileForm" size="small" label-width="90px" style="margin-top:16px">
        <el-form-item label="姓名"><el-input v-model.trim="profileForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model.trim="profileForm.phone" maxlength="11" /></el-form-item>
        <el-form-item label="关系">
          <el-select v-model="profileForm.relationship">
            <el-option v-for="item in relationships" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item><el-checkbox v-model="profileForm.defaultPatient">设为默认就诊人</el-checkbox></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button v-if="profileForm.id" @click="editProfile()">新增就诊人</el-button>
        <el-button type="primary" :loading="profileBusy" @click="saveProfile">{{ profileForm.id ? '保存修改' : '添加就诊人' }}</el-button>
      </span>
    </el-dialog>

    <el-dialog title="支付挂号费" :visible.sync="payVisible" width="420px" :close-on-click-modal="false" :show-close="false">
      <div v-if="created">
        <p style="font-size:14px;color:#4b5563;margin:0 0 14px">
          预约 <strong>#{{ created.id }}</strong> 已创建，当前状态为「待支付」。<br />
          应付金额：<strong style="color:#f56c6c;font-size:18px">{{ created.amount | money }}</strong>
        </p>
        <p v-if="created.paymentDeadline" class="muted">请在 {{ created.paymentDeadline }} 前支付，超时后自动释放号源。</p>
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

function emptyProfile() {
  return { name: '', phone: '', relationship: '本人', defaultPatient: false }
}

function bookingDate(daysAhead) {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default {
  name: 'PatientBook',
  data() {
    return {
      PAY_METHODS,
      profiles: [],
      profilesVisible: false,
      profileBusy: false,
      profileForm: emptyProfile(),
      relationships: ['本人', '父母', '子女', '配偶', '其他'],
      patientProfileId: null,
      departments: [],
      doctors: [],
      schedules: [],
      departmentId: '',
      doctorId: '',
      scheduleDate: bookingDate(1),
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
    patientApi.patients().then(list => {
      this.profiles = list
      const defaultPatient = list.find(p => p.defaultPatient)
      if (!this.$route.query.fromAppointment && !this.form.patientName && !this.form.patientPhone && defaultPatient) {
        this.patientProfileId = defaultPatient.id
      }
    }).catch(() => {})
    patientApi.departments().then(list => {
      this.departments = list || []
      if (this.$route.query.fromAppointment) return this.loadRebooking()
    }).catch(() => {})
  },
  methods: {
    async loadRebooking() {
      const id = this.$route.query.fromAppointment
      if (!/^[1-9][0-9]*$/.test(id)) return
      const source = await patientApi.appointmentDetail(id)
      if (source.cancellationType !== 'schedule_stopped') return
      if (!this.form.patientName && !this.form.patientPhone && !this.patientProfileId) {
        this.form.patientName = source.patientName
        this.form.patientPhone = source.patientPhone
      }
      if (this.departmentId || !this.departments.some(d => d.id === source.departmentId)) return
      this.departmentId = source.departmentId
      const today = bookingDate(0)
      this.scheduleDate = source.scheduleDate >= today ? source.scheduleDate : today
      await this.onDepartmentChange()
      if (this.departmentId === source.departmentId && !this.doctorId && this.doctors.some(d => d.id === source.doctorId)) {
        this.doctorId = source.doctorId
        this.loadSchedules()
      }
    },
    openProfiles() {
      this.editProfile()
      this.profilesVisible = true
      patientApi.patients().then(this.updateProfiles).catch(() => {})
    },
    updateProfiles(list) {
      this.profiles = list
      if (!list.some(p => p.id === this.patientProfileId)) this.patientProfileId = null
    },
    editProfile(row) {
      this.profileForm = row ? { ...row } : emptyProfile()
    },
    async saveProfile() {
      if (this.profileBusy) return
      const { name, phone } = this.profileForm
      if (!name || !/^1[3-9]\d{9}$/.test(phone)) {
        this.$message.warning('请填写姓名和有效的 11 位手机号')
        return
      }
      this.profileBusy = true
      try {
        this.updateProfiles(await patientApi.savePatient(this.profileForm))
        const saved = this.profiles.find(p => p.name === name && p.phone === phone)
        if (saved) this.patientProfileId = saved.id
        this.editProfile()
        this.profilesVisible = false
      } catch (error) {
        // 通用请求拦截器展示错误。
      } finally {
        this.profileBusy = false
      }
    },
    async removeProfile(row) {
      try {
        await this.$confirm('删除后，已生成的预约会保留原有就诊信息。', '删除常用就诊人')
      } catch (error) {
        return
      }
      if (this.profileBusy) return
      this.profileBusy = true
      try {
        this.updateProfiles(await patientApi.removePatient(row.id))
        if (this.profileForm.id === row.id) this.editProfile()
      } catch (error) {
        // 通用请求拦截器展示错误。
      } finally {
        this.profileBusy = false
      }
    },
    onDepartmentChange() {
      this.doctorId = ''
      this.scheduleId = ''
      this.doctors = []
      this.schedules = []
      const departmentId = this.departmentId
      if (!departmentId) return
      return patientApi.doctors({ departmentId })
        .then(list => {
          if (this.departmentId === departmentId) this.doctors = list || []
        })
        .catch(() => {})
    },
    loadSchedules() {
      this.scheduleId = ''
      this.schedules = []
      if (!this.doctorId || !this.scheduleDate) return
      const requestId = this.scheduleRequest = (this.scheduleRequest || 0) + 1
      const { doctorId, scheduleDate } = this
      this.loadingSchedules = true
      patientApi.schedules({ doctorId, scheduleDate })
        .then(list => {
          if (this.scheduleRequest === requestId && this.doctorId === doctorId && this.scheduleDate === scheduleDate) {
            this.schedules = list || []
          }
        })
        .catch(() => {})
        .finally(() => {
          if (this.scheduleRequest === requestId) this.loadingSchedules = false
        })
    },
    submit() {
      if (this.submitting) return
      let valid = Boolean(this.patientProfileId)
      if (!valid) this.$refs.form.validate(v => { valid = v })
      if (!valid) return
      if (!this.scheduleId) { this.$message.warning('请先选择就诊号源'); return }
      this.submitting = true
      const selected = this.availableSchedules.find(s => s.id === this.scheduleId)
      if (!selected) { this.submitting = false; return }
      const booking = { scheduleId: selected.id, expectedRegistrationFee: selected.registrationFee }
      const payload = this.patientProfileId
        ? { ...booking, patientProfileId: this.patientProfileId, remark: this.form.remark }
        : { ...booking, ...this.form }
      patientApi.createAppointment(payload)
        .then(data => {
          this.created = data
          this.payVisible = true
        })
        .catch(() => { this.loadSchedules() })
        .finally(() => { this.submitting = false })
    },
    pay() {
      if (!this.created || this.paying) return
      this.paying = true
      patientApi.payAppointment(this.created.id, this.payMethod)
        .then(() => {
          this.$message.success('支付成功，预约已生效')
          this.payVisible = false
          this.$router.push('/patient/appointments')
        })
        .catch(() => patientApi.appointmentDetail(this.created.id).then(detail => {
          if (detail.paymentExpired || detail.status !== 'unpaid') {
            this.payVisible = false
            this.$router.push('/patient/appointments')
          }
        }).catch(() => {}))
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
