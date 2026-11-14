<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">余额与充值</h2>
        <p class="page-desc">充值按课程项目规则为模拟支付（微信 / 支付宝），余额可用于挂号费与药费</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="panel">
          <div class="stat-label">当前可用余额</div>
          <div class="stat-value" style="color:#1d6fe0">{{ balance | money }}</div>
          <p class="page-desc" style="margin-top:8px">账号：{{ user.nickname || user.username }}</p>
          <el-divider />
          <el-form ref="form" :model="form" :rules="rules" label-width="80px" size="small">
            <el-form-item label="充值金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :max="10000" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="支付方式" prop="payMethod">
              <el-select v-model="form.payMethod" style="width:100%">
                <el-option label="微信支付（模拟）" value="wechat" />
                <el-option label="支付宝（模拟）" value="alipay" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-button type="primary" icon="el-icon-money" :loading="recharging" @click="recharge">确认充值</el-button>
          <div class="muted" style="font-size:12px;margin-top:12px">单次充值范围 0.01 ~ 10000.00 元，服务端会写入余额流水。</div>
        </div>
      </el-col>
      <el-col :span="16">
        <div class="panel" v-loading="loading">
          <h3 style="margin:0 0 14px;font-size:16px">余额流水</h3>
          <el-table :data="rows" border stripe size="small">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column label="变动金额" width="110" align="right">
              <template slot-scope="{ row }">
                <span :style="{ color: Number(row.amount) >= 0 ? '#e34d4d' : '#3aa76d' }">{{ Number(row.amount) >= 0 ? '+' : '' }}{{ row.amount | money }}</span>
              </template>
            </el-table-column>
            <el-table-column label="变动后余额" width="120" align="right">
              <template slot-scope="{ row }">{{ row.balanceAfter | money }}</template>
            </el-table-column>
            <el-table-column prop="payMethodText" label="方式" width="110">
              <template slot-scope="{ row }">{{ payMethodText(row.payMethod) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间" width="170">
              <template slot-scope="{ row }">{{ row.createTime | time }}</template>
            </el-table-column>
            <template slot="empty">暂无流水</template>
          </el-table>
          <div class="pagination">
            <el-pagination background layout="total, sizes, prev, pager, next"
              :current-page="current" :page-size="size" :page-sizes="[10, 20, 50]" :total="total"
              @size-change="handleSizeChange" @current-change="handleCurrentChange" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { patientApi } from '@/api/patient'
import { authApi } from '@/api/admin'
import { payMethodText } from '@/utils/format'
import { setAuth, getToken } from '@/utils/auth'

export default {
  name: 'PatientWallet',
  mixins: [listPage],
  data() {
    return {
      balance: 0,
      user: {},
      recharging: false,
      form: { amount: 100, payMethod: 'wechat' },
      rules: {
        amount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }],
        payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
      }
    }
  },
  methods: {
    payMethodText,
    defaultQuery() { return {} },
    fetchList(params) { return patientApi.walletLogs(params) },
    saveData() { return Promise.resolve() },
    pageCreated() { this.loadBalance() },
    loadBalance() {
      authApi.me().then(info => {
        if (!info) return
        this.user = info
        this.balance = info.balance || 0
        setAuth({ token: getToken(), user: info })
      }).catch(() => {})
    },
    recharge() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.recharging = true
        patientApi.recharge(this.form)
          .then(result => {
            this.$message.success(`充值成功，当前余额 ${Number((result && result.balance) || 0).toFixed(2)} 元`)
            this.loadBalance()
            this.loadData()
          })
          .catch(() => {})
          .finally(() => { this.recharging = false })
      })
    }
  }
}
</script>
