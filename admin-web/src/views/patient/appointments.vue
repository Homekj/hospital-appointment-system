<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的预约</h2>
        <p class="page-desc">挂号记录、支付、签到、取消与评价，全部对接 /api/appointments</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            <el-option label="待取药" value="pending_dispense" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <el-button type="primary" icon="el-icon-circle-plus" @click="$router.push('/patient/book')">预约挂号</el-button>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="75" />
        <el-table-column prop="doctorName" label="医生" width="110" />
        <el-table-column prop="departmentName" label="科室" min-width="110" />
        <el-table-column label="就诊日期" width="125">
          <template slot-scope="{ row }">{{ row.scheduleDate | date }}</template>
        </el-table-column>
        <el-table-column prop="timeSlot" label="时段" width="80" align="center" />
        <el-table-column label="挂号费" width="95" align="right">
          <template slot-scope="{ row }">{{ row.amount | money }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" effect="plain" :type="statusTagType(row.status)">{{ row.status | statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payMethodText" label="支付方式" width="105" />
        <el-table-column label="操作" width="290" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'unpaid'" type="text" icon="el-icon-money" @click="openPay(row)">支付</el-button>
            <el-button v-if="['unpaid', 'pending'].includes(row.status)" type="text" icon="el-icon-close" class="danger-link" @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status === 'pending'" type="text" icon="el-icon-location" @click="checkIn(row)">签到</el-button>
            <el-button v-if="row.status === 'completed' && !row.evaluated" type="text" icon="el-icon-star-off" @click="openReview(row)">评价</el-button>
            <el-button v-if="['pending_rx_pay', 'completed'].includes(row.status)" type="text" icon="el-icon-document" @click="goPrescription(row)">处方</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无预约记录</template>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :current-page="current" :page-size="size" :page-sizes="[10, 20, 50]" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </div>

    <el-dialog title="预约详情" :visible.sync="detailVisible" width="600px">
      <div v-loading="detailLoading" class="detail-list">
        <el-row :gutter="12">
          <el-col :span="12"><p><strong>预约编号</strong>{{ detail.id }}</p></el-col>
          <el-col :span="12"><p><strong>状态</strong>{{ detail.status | statusText }}</p></el-col>
          <el-col :span="12"><p><strong>就诊人</strong>{{ detail.patientName }}</p></el-col>
          <el-col :span="12"><p><strong>手机号</strong>{{ detail.patientPhone | maskPhone }}</p></el-col>
          <el-col :span="12"><p><strong>科室</strong>{{ detail.departmentName }}</p></el-col>
          <el-col :span="12"><p><strong>医生</strong>{{ detail.doctorName }} {{ detail.doctorTitle }}</p></el-col>
          <el-col :span="12"><p><strong>就诊日期</strong>{{ detail.scheduleDate | date }}</p></el-col>
          <el-col :span="12"><p><strong>时段</strong>{{ detail.timeSlot }}</p></el-col>
          <el-col :span="12"><p><strong>挂号费</strong>{{ detail.amount | money }}</p></el-col>
          <el-col :span="12"><p><strong>支付方式</strong>{{ detail.payMethodText || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>证件号</strong>{{ detail.idCard || '未填写' }}</p></el-col>
          <el-col :span="12"><p><strong>下单时间</strong>{{ detail.createTime | time }}</p></el-col>
          <el-col :span="24"><p><strong>备注</strong>{{ detail.remark || '-' }}</p></el-col>
        </el-row>
      </div>
      <span slot="footer"><el-button type="primary" @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog title="支付挂号费" :visible.sync="payVisible" width="400px" :close-on-click-modal="false">
      <p style="margin:0 0 12px;font-size:14px">预约 #{{ payTarget && payTarget.id }}，应付 <strong>{{ (payTarget && payTarget.amount) | money }}</strong></p>
      <el-radio-group v-model="payMethod">
        <el-radio v-for="m in PAY_METHODS" :key="m.value" :label="m.value" style="display:block;margin:8px 0">{{ m.label }}</el-radio>
      </el-radio-group>
      <span slot="footer">
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="pay">确认支付</el-button>
      </span>
    </el-dialog>

    <el-dialog title="评价本次就诊" :visible.sync="reviewVisible" width="440px">
      <el-form label-width="80px" size="small">
        <el-form-item label="评分"><el-rate v-model="reviewForm.rating" :max="5" show-text /></el-form-item>
        <el-form-item label="标签">
          <el-checkbox-group v-model="reviewForm.tags">
            <el-checkbox v-for="t in REVIEW_TAGS" :key="t" :label="t" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="评语"><el-input v-model="reviewForm.comment" type="textarea" :rows="3" maxlength="500" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">提交评价</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { patientApi } from '@/api/patient'
import { STATUS_OPTIONS, PAY_METHODS, statusTagType, payMethodText } from '@/utils/format'

const REVIEW_TAGS = ['态度好', '专业', '耐心', '效率高', '环境好']

export default {
  name: 'PatientAppointments',
  mixins: [listPage],
  data() {
    return {
      STATUS_OPTIONS, PAY_METHODS, REVIEW_TAGS,
      detailVisible: false, detailLoading: false, detail: {},
      payVisible: false, paying: false, payMethod: 'wechat', payTarget: null,
      reviewVisible: false, reviewing: false, reviewTarget: null,
      reviewForm: { rating: 5, tags: [], comment: '' }
    }
  },
  methods: {
    payMethodText, statusTagType,
    defaultQuery() { return { status: '' } },
    fetchList(params) { return patientApi.appointments(params) },
    saveData() { return Promise.resolve() },
    showDetail(row) {
      this.detail = {}
      this.detailVisible = true
      this.detailLoading = true
      patientApi.appointmentDetail(row.id)
        .then(data => { this.detail = data || {} })
        .catch(() => {})
        .finally(() => { this.detailLoading = false })
    },
    openPay(row) {
      this.payTarget = row
      this.payMethod = 'wechat'
      this.payVisible = true
    },
    pay() {
      if (!this.payTarget) return
      this.paying = true
      patientApi.payAppointment(this.payTarget.id, this.payMethod)
        .then(() => { this.$message.success('支付成功'); this.payVisible = false; this.loadData() })
        .catch(() => {})
        .finally(() => { this.paying = false })
    },
    cancel(row) {
      this.$confirm(`确定取消预约 #${row.id} 吗？号源将释放，余额支付会原路退回。`, '取消预约', { type: 'warning' })
        .then(() => patientApi.cancelAppointment(row.id))
        .then(() => { this.$message.success('预约已取消'); this.loadData() })
        .catch(() => {})
    },
    checkIn(row) {
      patientApi.checkIn(row.id)
        .then(() => { this.$message.success('签到成功'); this.loadData() })
        .catch(() => {})
    },
    goPrescription(row) {
      this.$router.push({ path: '/patient/clinical', query: { appointmentId: row.id } })
    },
    openReview(row) {
      this.reviewTarget = row
      this.reviewForm = { rating: 5, tags: [], comment: '' }
      this.reviewVisible = true
    },
    submitReview() {
      if (!this.reviewTarget) return
      this.reviewing = true
      patientApi.review(this.reviewTarget.id, this.reviewForm)
        .then(() => { this.$message.success('评价已提交'); this.reviewVisible = false; this.loadData() })
        .catch(() => {})
        .finally(() => { this.reviewing = false })
    }
  }
}
</script>
