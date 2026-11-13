<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">消息通知</h2>
        <p class="page-desc">站内消息，未读 <el-badge :value="unread" :hidden="!unread" type="primary" /> 条，对接 /api/notifications</p>
      </div>
      <el-button icon="el-icon-check" :disabled="!unread" @click="readAll">全部标为已读</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-select v-model="query.read" placeholder="全部消息" clearable style="width: 150px">
            <el-option label="未读" :value="false" />
            <el-option label="已读" :value="true" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <el-button icon="el-icon-refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" effect="plain" :type="row.read ? 'info' : 'warning'">{{ row.read ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="时间" width="175">
          <template slot-scope="{ row }">{{ row.createTime | time }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button v-if="!row.read" type="text" @click="read(row)">标为已读</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无消息</template>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :current-page="current" :page-size="size" :page-sizes="[10, 20, 50]" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </div>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { patientApi } from '@/api/patient'

export default {
  name: 'PatientNotifications',
  mixins: [listPage],
  data() {
    return { unread: 0 }
  },
  methods: {
    defaultQuery() { return { read: '' } },
    fetchList(params) { return patientApi.notifications(params) },
    saveData() { return Promise.resolve() },
    pageCreated() { this.loadUnread() },
    loadUnread() {
      patientApi.unreadCount().then(count => { this.unread = Number(count || 0) }).catch(() => {})
    },
    read(row) {
      patientApi.readNotification(row.id)
        .then(() => { this.loadData(); this.loadUnread() })
        .catch(() => {})
    },
    readAll() {
      this.$confirm(`将全部 ${this.unread} 条未读消息标为已读？`, '提示', { type: 'info' })
        .then(() => patientApi.readAll())
        .then(() => { this.$message.success('已全部标为已读'); this.loadData(); this.loadUnread() })
        .catch(() => {})
    }
  }
}
</script>
