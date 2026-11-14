<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">医生管理</h2>
        <p class="page-desc">新增医生会自动创建登录账号并联动启停用，数据来自 /api/admin/doctors</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <div class="filters">
          <el-input v-model="query.keyword" placeholder="医生姓名" clearable style="width: 180px" @keyup.enter.native="handleSearch" />
          <el-select v-model="query.departmentId" placeholder="所属科室" clearable filterable style="width: 170px">
            <el-option v-for="d in departmentOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="resetQuery">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增医生</el-button>
        </div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="头像" width="78" align="center">
          <template slot-scope="{ row }">
            <el-avatar :size="36" :src="resolveDoctorAvatar(row.avatar)">{{ (row.name || '医').slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="departmentName" label="科室" min-width="110" />
        <el-table-column prop="title" label="职称" min-width="120" />
        <el-table-column label="性别" width="80" align="center">
          <template slot-scope="{ row }">{{ row.gender === 2 ? '女' : '男' }}</template>
        </el-table-column>
        <el-table-column prop="username" label="登录账号" min-width="140" />
        <el-table-column prop="intro" label="简介" min-width="200" show-overflow-tooltip />
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
        <template slot="empty">暂无医生数据</template>
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

    <el-dialog :title="dialogMode === 'create' ? '新增医生' : '编辑医生'" :visible.sync="dialogVisible" width="640px" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="所属科室" prop="departmentId">
            <el-select v-model="form.departmentId" placeholder="请选择科室" filterable style="width: 100%">
              <el-option v-for="d in departmentOptions" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="医生姓名" />
          </el-form-item>
          <el-form-item label="职称" prop="title">
            <el-input v-model="form.title" maxlength="50" placeholder="如：主任医师" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio :label="1">男</el-radio>
              <el-radio :label="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="头像路径" prop="avatar">
            <div class="doctor-avatar-editor">
              <el-avatar :size="56" :src="resolveDoctorAvatar(form.avatar)">{{ (form.name || '医').slice(0, 1) }}</el-avatar>
              <el-upload
                action="#"
                accept="image/jpeg,image/png,image/webp"
                :show-file-list="false"
                :before-upload="beforeUploadDoctorAvatar"
                :http-request="uploadDoctorAvatar"
              >
                <el-button size="small" icon="el-icon-upload2" :loading="avatarUploading">上传医生头像</el-button>
              </el-upload>
            </div>
            <el-input v-model="form.avatar" maxlength="255" placeholder="上传后自动填写，也可手动填写现有图片路径" />
          </el-form-item>
          <el-form-item label="医生简介" prop="intro" class="form-full">
            <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="1000" placeholder="擅长领域" />
          </el-form-item>
        </div>
      </el-form>
      <span slot="footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="医生账号已创建" :visible.sync="accountVisible" width="420px">
      <div class="detail-list">
        <p><strong>登录账号</strong>{{ createdAccount.username }}</p>
        <p><strong>初始密码</strong>{{ createdAccount.defaultPassword }}</p>
        <p><strong>说明</strong>{{ createdAccount.message }}</p>
      </div>
      <span slot="footer"><el-button type="primary" @click="accountVisible = false">知道了</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import listPage from '@/mixins/listPage'
import { doctorApi, catalogApi } from '@/api/admin'
import request from '@/utils/request'

export default {
  name: 'DoctorsPage',
  mixins: [listPage],
  data() {
    return {
      departmentOptions: [],
      avatarUploading: false,
      accountVisible: false,
      createdAccount: {},
      rules: {
        departmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }],
        name: [{ required: true, message: '请输入医生姓名', trigger: 'blur' }]
      }
    }
  },
  methods: {
    resolveDoctorAvatar(path) {
      if (!path) return ''
      if (/^https?:\/\//i.test(path)) return path
      return `${(import.meta.env.VITE_BACKEND_URL || '').replace(/\/$/, '')}${path}`
    },
    beforeUploadDoctorAvatar(file) {
      const allowed = ['image/jpeg', 'image/png', 'image/webp']
      if (!allowed.includes(file.type)) {
        this.$message.error('仅支持 jpg、png、webp 图片')
        return false
      }
      if (file.size > 5 * 1024 * 1024) {
        this.$message.error('头像不能超过 5MB')
        return false
      }
      return true
    },
    uploadDoctorAvatar(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      formData.append('category', 'avatar')
      this.avatarUploading = true
      request.post('/files/upload', formData)
        .then(result => {
          const path = (result && result.path) || (result && result.url) || ''
          if (!path) throw new Error('上传响应缺少文件路径')
          this.form.avatar = path
          this.$message.success('头像已上传，保存医生信息后生效')
        })
        .catch(() => {})
        .finally(() => { this.avatarUploading = false })
    },
    defaultQuery() { return { keyword: '', departmentId: '', status: '' } },
    defaultForm() {
      return { departmentId: '', name: '', title: '', gender: 1, avatar: '', intro: '', status: 1 }
    },
    pageCreated() {
      catalogApi.departments().then(list => { this.departmentOptions = list || [] }).catch(() => {})
    },
    fetchList(params) { return doctorApi.page(params) },
    saveData(form, isEdit, id) { return isEdit ? doctorApi.update(id, form) : doctorApi.create(form) },
    deleteData(row) { return doctorApi.remove(row.id) },
    toggle(row) { this.toggleStatus(row, doctorApi.status) },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.editId = row.id
      doctorApi.detail(row.id).then(detail => {
        this.form = Object.assign(this.defaultForm(), detail || row)
        this.dialogVisible = true
        this.$nextTick(() => { if (this.$refs.form) this.$refs.form.clearValidate() })
      }).catch(() => {})
    },
    afterSave(result) {
      if (result && result.username) {
        this.createdAccount = result
        this.accountVisible = true
      }
    }
  }
}
</script>

<style scoped>
.doctor-avatar-editor {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
</style>
