<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">处方与发药</h2>
        <p class="page-desc">核对处方中的药品、用法和数量，交付后确认发药</p>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <div class="toolbar">
        <el-select v-model="status" @change="search" placeholder="处方状态">
          <el-option label="全部状态" value="" />
          <el-option v-for="(label, value) in labels" :key="value" :label="label" :value="value" />
        </el-select>
        <el-input v-model.trim="lookupId" placeholder="处方编号" style="width:145px" @keyup.enter.native="findById" />
        <el-button icon="el-icon-search" @click="findById">查处方</el-button>
        <el-button icon="el-icon-refresh" @click="load">刷新</el-button>
      </div>
      <el-table :data="rows" empty-text="暂无符合条件的处方">
        <el-table-column prop="id" label="处方编号" width="100" />
        <el-table-column prop="appointmentId" label="预约编号" width="100" />
        <el-table-column prop="doctorName" label="医生" min-width="100" />
        <el-table-column prop="departmentName" label="科室" min-width="100" />
        <el-table-column label="金额" width="100"><template slot-scope="{ row }">{{ row.totalAmount | money }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template slot-scope="{ row }">{{ statusText(row) }}</template></el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="{ row }"><el-button type="text" @click="open(row)">查看处方</el-button></template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination layout="total, prev, pager, next" :total="total" :current-page="page"
          :page-size="size" @current-change="changePage" />
      </div>
    </div>

    <el-dialog title="处方详情" :visible.sync="visible" width="690px" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <p>处方 #{{ detail.id }} · 预约 #{{ detail.appointmentId }} · {{ statusText(detail) }}</p>
          <el-table :data="detail.items || []">
            <el-table-column prop="medicineName" label="药品" />
            <el-table-column prop="spec" label="规格" />
            <el-table-column prop="dosage" label="用量" />
            <el-table-column prop="frequency" label="频次" />
            <el-table-column prop="days" label="天数" width="65" />
            <el-table-column prop="quantity" label="数量" width="65" />
          </el-table>
          <p v-if="detail.refundTime">{{ detail.refundTime }} · {{ detail.refundReason }}</p>
        </template>
      </div>
      <span slot="footer">
        <el-button @click="visible = false">关闭</el-button>
        <template v-if="detail && detail.status === 'paid'">
          <el-button type="danger" plain :disabled="busy" @click="refund">取消并退费</el-button>
          <el-button type="primary" :loading="busy" @click="dispense">确认已发药</el-button>
        </template>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { prescriptionApi } from '@/api/admin'

export default {
  name: 'AdminPrescriptions',
  data() {
    return {
      labels: { unpaid: '待支付', paid: '待发药', dispensed: '已发药', cancelled: '已取消或退费' },
      status: 'paid',
      lookupId: '',
      page: 1,
      size: 15,
      total: 0,
      rows: [],
      loading: false,
      visible: false,
      detailLoading: false,
      detail: null,
      busy: false
    }
  },
  created() { this.load() },
  methods: {
    statusText(row) {
      return row.refundTime ? '已退费' : row.status === 'cancelled' ? '已取消' : this.labels[row.status]
    },
    search() { this.page = 1; this.load() },
    changePage(page) { this.page = page; this.load() },
    async load() {
      const requestId = this.requestId = (this.requestId || 0) + 1
      this.loading = true
      try {
        const result = await prescriptionApi.page({ status: this.status, current: this.page, size: this.size })
        if (this.requestId !== requestId) return
        this.rows = result.records || []
        this.total = Number(result.total || 0)
        if (!this.rows.length && this.page > 1) this.changePage(this.page - 1)
      } catch (error) {
        // 通用请求拦截器展示错误。
      } finally {
        if (this.requestId === requestId) this.loading = false
      }
    },
    findById() {
      if (!/^[1-9][0-9]*$/.test(this.lookupId)) {
        this.$message.warning('请输入有效的处方编号')
        return
      }
      this.open({ id: this.lookupId })
    },
    async open(row) {
      if (this.busy || this.detailLoading) return
      this.detail = null
      this.visible = true
      this.detailLoading = true
      try {
        this.detail = await prescriptionApi.detail(row.id)
      } catch (error) {
        this.visible = false
      } finally {
        this.detailLoading = false
      }
    },
    async dispense() {
      if (this.busy) return
      const { id } = this.detail
      try {
        await this.$confirm('确认药品和数量已核对，并已交付给就诊人？', '确认发药', { type: 'warning' })
      } catch (error) {
        return
      }
      await this.apply(() => prescriptionApi.dispense(id), '已登记发药')
    },
    async refund() {
      if (this.busy) return
      const { id, payMethod } = this.detail
      const refundText = payMethod === 'balance' ? '药费退回账户余额' : '登记模拟退款'
      try {
        const { value } = await this.$prompt('请填写患者可见的退费原因。此操作将' + refundText + '。', '退费并取消处方', {
          inputType: 'textarea',
          inputValidator: text => text && text.trim() && text.trim().length <= 200 || '请输入 1 至 200 个字符'
        })
        await this.apply(() => prescriptionApi.refund(id, value.trim()), '已登记退费')
      } catch (error) {
        // 用户取消。
      }
    },
    async apply(action, message) {
      if (this.busy) return
      this.busy = true
      try {
        this.detail = await action()
        this.$message.success(message)
        this.load()
      } catch (error) {
        // 通用请求拦截器展示错误。
      } finally {
        this.busy = false
      }
    }
  }
}
</script>
