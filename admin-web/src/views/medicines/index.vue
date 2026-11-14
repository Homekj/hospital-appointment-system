<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">药品管理</h2>
        <p class="page-desc">对接 /api/admin/medicines，已被处方引用的药品只能停用，不能物理删除</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="药品名称 / 规格" clearable style="width: 210px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增药品</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="药品名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="单价" width="110" align="right">
          <template slot-scope="{ row }">{{ row.price | money }}</template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.stock < 20 ? 'danger' : 'success'" effect="plain">{{ row.stock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usageDesc" label="用法说明" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" :fixed="$root.isMobile ? false : 'right'">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" :icon="row.status === 1 ? 'el-icon-turn-off' : 'el-icon-open'" @click="toggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-link" @click="confirmRemove(row, row.name)">删除</el-button>
          </template>
        </el-table-column>
        <template slot="empty">暂无药品数据</template>
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

    <el-dialog :title="dialogMode === 'create' ? '新增药品' : '编辑药品'" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="药品名称" prop="name">
            <el-input v-model="form.name" maxlength="100" />
          </el-form-item>
          <el-form-item label="规格" prop="spec">
            <el-input v-model="form.spec" maxlength="100" placeholder="如：0.25g*24粒" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model="form.unit" maxlength="20" placeholder="盒 / 瓶 / 支" />
          </el-form-item>
          <el-form-item label="单价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :max="999999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="用法说明" prop="usageDesc" class="form-full">
            <el-input v-model="form.usageDesc" type="textarea" :rows="2" maxlength="200" placeholder="口服，遵医嘱" />
          </el-form-item>
        </div>
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
import { medicineApi } from '@/api/admin'

export default {
  name: 'MedicinesPage',
  mixins: [listPage],
  data() {
    return {
      rules: {
        name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
        stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
      }
    }
  },
  methods: {
    defaultQuery() { return { keyword: '', status: '' } },
    defaultForm() { return { name: '', spec: '', unit: '盒', price: 0, stock: 0, usageDesc: '', status: 1 } },
    fetchList(params) { return medicineApi.page(params) },
    saveData(form, isEdit, id) { return isEdit ? medicineApi.update(id, form) : medicineApi.create(form) },
    deleteData(row) { return medicineApi.remove(row.id) },
    toggle(row) { this.toggleStatus(row, medicineApi.status) }
  }
}
</script>
