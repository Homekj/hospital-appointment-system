<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统日志</h2>
        <p class="page-desc">登录日志与操作日志分别对接 /api/admin/logs/login 与 /api/admin/logs/operation</p>
      </div>
      <el-button icon="el-icon-refresh" @click="reload">刷新</el-button>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab" @tab-click="reload">
        <el-tab-pane label="登录日志" name="login" />
        <el-tab-pane label="操作日志" name="operation" />
      </el-tabs>

      <div class="toolbar" style="margin-bottom: 14px">
        <div class="filters">
          <el-input v-model="filters.username" placeholder="操作账号" clearable style="width: 180px" @keyup.enter.native="handleSearch" />
          <el-select v-model="filters.status" placeholder="结果" clearable style="width: 130px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-input v-if="activeTab === 'operation'" v-model="filters.module" placeholder="模块" clearable style="width: 150px" />
          <el-date-picker v-model="filters.startDate" type="date" value-format="yyyy-MM-dd" placeholder="开始日期" style="width: 160px" />
          <el-date-picker v-model="filters.endDate" type="date" value-format="yyyy-MM-dd" placeholder="结束日期" style="width: 160px" />
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
        </div>
      </div>

      <el-table v-if="activeTab === 'login'" v-loading="loading" :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="登录账号" min-width="160" />
        <el-table-column prop="ip" label="IP" min-width="140" />
        <el-table-column label="结果" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180" />
        <template slot="empty">暂无登录日志</template>
      </el-table>

      <el-table v-else v-loading="loading" :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" min-width="120" />
        <el-table-column prop="module" label="模块" min-width="120" />
        <el-table-column prop="action" label="操作" min-width="120" />
        <el-table-column prop="method" label="方法" width="90" align="center" />
        <el-table-column prop="url" label="请求地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="130" />
        <el-table-column label="耗时" width="100" align="right">
          <template slot-scope="{ row }">{{ row.costMs != null ? row.costMs + ' ms' : '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <template slot="empty">暂无操作日志</template>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="current"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { logApi } from '@/api/admin'

export default {
  name: 'LogsPage',
  data() {
    return {
      activeTab: 'login',
      filters: { username: '', status: '', module: '', startDate: '', endDate: '' },
      rows: [],
      total: 0,
      current: 1,
      size: 10,
      loading: false
    }
  },
  created() {
    this.load()
  },
  methods: {
    params() {
      const result = { current: this.current, size: this.size }
      Object.keys(this.filters).forEach(key => {
        const value = this.filters[key]
        if (value !== '' && value !== null && value !== undefined) result[key] = value
      })
      return result
    },
    load() {
      this.loading = true
      const request = this.activeTab === 'login' ? logApi.login(this.params()) : logApi.operation(this.params())
      request
        .then(page => {
          this.rows = (page && page.records) || []
          this.total = Number((page && page.total) || 0)
        })
        .catch(() => { this.rows = []; this.total = 0 })
        .finally(() => { this.loading = false })
    },
    reload() {
      this.current = 1
      this.load()
    },
    handleSearch() { this.reload() },
    reset() {
      this.filters = { username: '', status: '', module: '', startDate: '', endDate: '' }
      this.reload()
    },
    handleSizeChange(size) { this.size = size; this.reload() },
    handleCurrentChange(page) { this.current = page; this.load() }
  }
}
</script>
