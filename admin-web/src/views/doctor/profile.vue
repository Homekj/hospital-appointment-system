<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2 class="page-title">个人资料</h2>
        <p class="page-desc">查看当前医生档案并维护登录密码</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="panel" v-loading="loading">
          <div class="doctor-profile-head">
            <el-avatar :size="88" :src="avatarUrl">{{ (profile.name || '医').slice(0, 1) }}</el-avatar>
            <div>
              <h3 class="doctor-profile-name">{{ profile.name || '医生' }}</h3>
              <el-upload action="#" :show-file-list="false" :http-request="uploadAvatar" accept=".jpg,.jpeg,.png,.webp" :before-upload="beforeAvatarUpload">
                <el-button size="small" icon="el-icon-upload2" :loading="uploading">上传/更换头像</el-button>
              </el-upload>
              <p class="muted upload-hint">支持 JPG、PNG、WebP，最大 5MB</p>
            </div>
          </div>
          <h3 class="section-title">医生档案</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ profile.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ profile.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="科室">{{ profile.departmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ profile.gender || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号">{{ user.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ Number(profile.status) === 1 ? '在职' : '停用' }}</el-descriptions-item>
            <el-descriptions-item label="简介">{{ profile.intro || '暂无简介' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>

      <el-col :xs="24" :md="12">
        <div class="panel">
          <h3 class="section-title">修改登录密码</h3>
          <el-form ref="passwordForm" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="updatePassword">保存新密码</el-button>
            </el-form-item>
          </el-form>
          <p class="muted password-hint">新密码为 8–72 个字符，且最多 72 个 UTF-8 字节；修改时需要先验证当前密码。</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { doctorApi } from '@/api/doctor'
import { authApi } from '@/api/admin'
import request from '@/utils/request'
import { getUser, getToken, setAuth } from '@/utils/auth'

export default {
  name: 'DoctorProfile',
  data() {
    const sameAsNewPassword = (rule, value, callback) => {
      if (value !== this.form.newPassword) callback(new Error('两次输入的新密码不一致'))
      else callback()
    }
    return {
      profile: {},
      user: getUser(),
      loading: false,
      saving: false,
      uploading: false,
      form: { currentPassword: '', newPassword: '', confirmPassword: '' },
      rules: {
        currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, max: 72, message: '新密码长度须为 8 到 72 个字符', trigger: 'blur' }
        ],
        confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: sameAsNewPassword, trigger: 'blur' }]
      }
    }
  },
  computed: {
    avatarUrl() {
      const path = this.user.avatar || this.profile.avatar || ''
      if (!path) return ''
      return path.startsWith('http') ? path : `${(import.meta.env.VITE_BACKEND_URL || '').replace(/\/$/, '')}${path}`
    }
  },
  created() {
    this.loading = true
    Promise.all([doctorApi.me(), authApi.me()])
      .then(([profile, user]) => { this.profile = profile || {}; this.user = user || this.user })
      .finally(() => { this.loading = false })
  },
  methods: {
    beforeAvatarUpload(file) {
      const validType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
      if (!validType) this.$message.error('仅支持 JPG、PNG、WebP 图片')
      if (file.size > 5 * 1024 * 1024) this.$message.error('图片不能超过 5MB')
      return validType && file.size <= 5 * 1024 * 1024
    },
    uploadAvatar(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      formData.append('category', 'avatar')
      this.uploading = true
      request.post('/files/upload', formData)
        .then(result => {
          const path = (result && (result.path || result.url)) || ''
          if (!path) throw new Error('上传响应缺少文件路径')
          return doctorApi.updateAvatar(path)
        })
        .then(user => {
          this.user = user
          setAuth({ token: getToken(), user })
          this.$message.success('头像已更新')
        })
        .catch(() => {})
        .finally(() => { this.uploading = false })
    },
    updatePassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) return
        this.saving = true
        doctorApi.updatePassword({ currentPassword: this.form.currentPassword, newPassword: this.form.newPassword })
          .then(() => {
            this.$message.success('密码已修改')
            this.form = { currentPassword: '', newPassword: '', confirmPassword: '' }
            this.$refs.passwordForm.clearValidate()
          })
          .finally(() => { this.saving = false })
      })
    }
  }
}
</script>

<style scoped>
.section-title { margin: 0 0 16px; font-size: 16px; }
.doctor-profile-head { display: flex; align-items: center; gap: 18px; margin-bottom: 22px; }
.doctor-profile-name { margin: 0 0 10px; font-size: 20px; }
.upload-hint { margin: 8px 0 0; font-size: 12px; }
.password-hint { font-size: 12px; margin: -8px 0 0 100px; }
</style>
