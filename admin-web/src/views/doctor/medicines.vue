<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">药品目录</h2>
        <p class="page-desc">开方可选药品（GET /api/doctor/medicines，仅启用且有库存的药品）</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="keyword" placeholder="药品名称关键字" clearable style="width: 220px" @keyup.enter.native="load" />
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="keyword = ''; load()">重置</el-button>
        </div>
        <el-button type="text" @click="$router.push('/doctor/consultation')">前往接诊工作台开方 →</el-button>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="unit" label="单位" width="90" align="center" />
        <el-table-column label="单价" width="110" align="right">
          <template slot-scope="{ row }">{{ row.price | money }}</template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template slot-scope="{ row }"><el-tag size="small" effect="plain" :type="row.stock < 20 ? 'danger' : 'success'">{{ row.stock }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="usageDesc" label="用法说明" min-width="200" show-overflow-tooltip />
        <template slot="empty">暂无药品</template>
      </el-table>
    </div>
  </div>
</template>

<script>
import { doctorApi } from '@/api/doctor'

export default {
  name: 'DoctorMedicines',
  data() {
    return { keyword: '', rows: [], loading: false }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      doctorApi.medicines(this.keyword)
        .then(list => { this.rows = list || [] })
        .catch(() => { this.rows = [] })
        .finally(() => { this.loading = false })
    }
  }
}
</script>
