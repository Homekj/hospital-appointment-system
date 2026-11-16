<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理员可查看用户账号并启停用；管理员账号不可禁用，密码与 openid 不下发前端</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="账号 / 昵称 / 手机号" clearable style="width: 220px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="患者" value="USER" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="登录账号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="110" />
        <el-table-column label="角色" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="roleType(row.role)" effect="plain">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template slot-scope="{ row }">{{ row.phone | maskPhone }}</template>
        </el-table-column>
        <el-table-column label="账户余额" width="120" align="right">
          <template slot-scope="{ row }">{{ row.balance | money }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="160" :fixed="$root.isMobile ? false : 'right'">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.role !== 'ADMIN' || row.status !== 1" type="text" :icon="row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="toggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <span v-else class="muted">管理员不可禁用</span>
          </template>
        </el-table-column>
        <template slot="empty">暂无用户数据</template>
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

    <el-dialog title="用户详情" :visible.sync="detailVisible" width="560px">
      <div class="detail-list">
        <el-row :gutter="12">
          <el-col :span="12"><p><strong>用户 ID</strong>{{ detail.id }}</p></el-col>
          <el-col :span="12"><p><strong>登录账号</strong>{{ detail.username }}</p></el-col>
          <el-col :span="12"><p><strong>昵称</strong>{{ detail.nickname || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>角色</strong>{{ roleLabel(detail.role) }}</p></el-col>
          <el-col :span="12"><p><strong>手机号</strong>{{ detail.phone || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>邮箱</strong>{{ detail.email || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>账户余额</strong>{{ detail.balance | money }}</p></el-col>
          <el-col :span="12"><p><strong>绑定医生</strong>{{ detail.doctorId || '-' }}</p></el-col>
          <el-col :span="12"><p><strong>状态</strong>{{ detail.status === 1 ? '启用' : '禁用' }}</p></el-col>
          <el-col :span="12"><p><strong>注册时间</strong>{{ detail.createTime }}</p></el-col>
        </el-row>
      </div>
      <span slot="footer"><el-button type="primary" @click="detailVisible = false">关闭</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { userApi } from '@/api/admin'

const ROLES = { ADMIN: '管理员', DOCTOR: '医生', USER: '患者' }

export default {
  name: 'UsersPage',
  mixins: [listPage],
  data() {
    return { detailVisible: false, detail: {} }
  },
  methods: {
    defaultQuery() { return { keyword: '', role: '', status: '' } },
    fetchList(params) { return userApi.page(params) },
    saveData() { return Promise.resolve() },
    toggle(row) { this.toggleStatus(row, userApi.status) },
    roleLabel(role) { return ROLES[role] || role || '-' },
    roleType(role) { return role === 'ADMIN' ? 'danger' : role === 'DOCTOR' ? 'primary' : 'success' },
    showDetail(row) {
      this.detail = Object.assign({}, row)
      this.detailVisible = true
      userApi.detail(row.id).then(data => { if (data) this.detail = data }).catch(() => {})
    }
  }
}
</script>
