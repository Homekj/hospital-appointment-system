<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">科室管理</h2>
        <p class="page-desc">对接 /api/admin/departments，支持分页查询、新增、修改、启停用与删除</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="科室名称" clearable style="width: 200px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增科室</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="科室名称" min-width="140" />
        <el-table-column prop="description" label="科室简介" min-width="200" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" min-width="130" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" :icon="row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="toggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-link" @click="confirmRemove(row, row.name)">删除</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无科室数据</template>
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

    <el-dialog :title="dialogMode === 'create' ? '新增科室' : '编辑科室'" :visible.sync="dialogVisible" width="620px" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="form.name" placeholder="如：内科" maxlength="100" />
        </el-form-item>
        <el-form-item label="科室简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" placeholder="主要诊疗范围" />
        </el-form-item>
        <el-form-item label="所在位置" prop="location">
          <el-input v-model="form.location" maxlength="200" placeholder="如：门诊楼3楼" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { departmentApi } from '@/api/admin'

export default {
  name: 'DepartmentsPage',
  mixins: [listPage],
  data() {
    return {
      rules: {
        name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
      }
    }
  },
  methods: {
    defaultQuery() { return { keyword: '', status: '' } },
    defaultForm() { return { name: '', description: '', location: '', sortOrder: 1, status: 1 } },
    fetchList(params) { return departmentApi.page(params) },
    saveData(form, isEdit, id) { return isEdit ? departmentApi.update(id, form) : departmentApi.create(form) },
    deleteData(row) { return departmentApi.remove(row.id) },
    toggle(row) { this.toggleStatus(row, departmentApi.status) }
  }
}
</script>

<style scoped>
.danger-link { color: #f56c6c; }
</style>
