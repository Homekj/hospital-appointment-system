<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">菜单管理</h2>
        <p class="page-desc">后台侧边栏菜单直接从 GET /api/admin/menus 读取，与 sys_menu 表保持一致</p>
      </div>
      <el-button icon="el-icon-refresh" @click="load">刷新</el-button>
    </div>

    <div class="panel" v-loading="loading">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
        title="菜单为管理员只读视图；如需调整菜单项，请在数据库 sys_menu 表中维护。" />
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="parentId" label="父级 ID" width="100" align="center" />
        <el-table-column prop="name" label="菜单名称" min-width="150" />
        <el-table-column prop="path" label="前端路由" min-width="200" />
        <el-table-column prop="icon" label="图标" min-width="150" />
        <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
        <el-table-column prop="roles" label="可见角色" width="120" align="center">
          <template slot-scope="{ row }"><el-tag size="small" effect="plain">{{ row.roles }}</el-tag></template>
        </el-table-column>
        <template slot="empty">暂无菜单数据</template>
      </el-table>
    </div>
  </div>
</template>

<script>
import { dashboardApi } from '@/api/admin'

export default {
  name: 'MenusPage',
  data() {
    return { rows: [], loading: false }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      dashboardApi.menus()
        .then(list => { this.rows = list || [] })
        .catch(() => { this.rows = [] })
        .finally(() => { this.loading = false })
    }
  }
}
</script>
