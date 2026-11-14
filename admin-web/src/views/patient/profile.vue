<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="page-title">个人资料</h2>
        <p class="page-desc">修改昵称、联系方式与头像，对接 /api/profile 与 /api/files/upload</p>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="panel" style="text-align:center">
          <el-avatar :size="96" :src="avatarUrl">{{ (user.nickname || '患').slice(0, 1) }}</el-avatar>
          <h3 style="margin:14px 0 4px">{{ user.nickname || user.username }}</h3>
          <el-tag size="small" effect="plain" type="success">患者账号</el-tag>
          <div style="margin-top:18px">
            <el-upload action="#" :show-file-list="false" :http-request="uploadAvatar" accept="image/*" :before-upload="beforeUpload">
              <el-button size="small" icon="el-icon-upload2" :loading="uploading">更换头像</el-button>
            </el-upload>
          </div>
          <p class="muted" style="font-size:12px;margin-top:12px">支持 jpg / png，最大 5MB</p>
        </div>
        <div class="panel detail-list">
          <p><strong>登录账号</strong>{{ user.username }}</p>
          <p><strong>账户余额</strong>{{ user.balance | money }}</p>
          <p><strong>账号状态</strong>{{ user.status === 1 ? '正常' : '禁用' }}</p>
        </div>
      </el-col>

      <el-col :span="16">
        <div class="panel">
          <h3 style="margin:0 0 16px;font-size:16px">基本信息</h3>
          <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width:520px">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" maxlength="50" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" maxlength="100" placeholder="example@mail.com" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="20" />
            </el-form-item>
            <el-form-item label="头像路径">
              <el-input v-model="form.avatar" maxlength="255" placeholder="/uploads/avatar/xxx.jpg" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
              <el-button @click="load">重新读取</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { authApi } from '@/api/admin'
import { patientApi } from '@/api/patient'
import request from '@/utils/request'
import { setAuth, getToken } from '@/utils/auth'

export default {
  name: 'PatientProfile',
  data() {
    return {
      loading: false,
      saving: false,
      uploading: false,
      user: {},
      form: { nickname: '', email: '', phone: '', avatar: '' },
      rules: {
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
        email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
      }
    }
  },
  computed: {
    avatarUrl() {
      const path = this.form.avatar || ''
      if (!path) return ''
      return path.startsWith('http') ? path : `${(import.meta.env.VITE_BACKEND_URL || '').replace(/\/$/, '')}${path}`
    }
  },
  created() { this.load() },
  methods: {
    load() {
      this.loading = true
      authApi.me()
        .then(info => {
          this.user = info || {}
          this.form = {
            nickname: info && info.nickname || '',
            email: info && info.email || '',
            phone: info && info.phone || '',
            avatar: info && info.avatar || ''
          }
        })
        .catch(() => {})
        .finally(() => { this.loading = false })
    },
    save() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const payload = {
          nickname: this.form.nickname,
          email: this.form.email || null,
          phone: this.form.phone || null
        }
        const tasks = [patientApi.updateProfile(payload)]
        if (this.form.avatar && this.form.avatar !== this.user.avatar) {
          tasks.push(patientApi.updateAvatar(this.form.avatar))
        }
        Promise.all(tasks)
          .then(results => {
            const latest = results[results.length - 1]
            if (latest) {
              this.user = latest
              setAuth({ token: getToken(), user: latest })
            }
            this.$message.success('资料已保存')
            this.load()
          })
          .catch(() => {})
          .finally(() => { this.saving = false })
      })
    },
    beforeUpload(file) {
      const ok = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
      if (!ok) this.$message.error('仅支持图片文件')
      return ok
    },
    uploadAvatar(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      formData.append('category', 'avatar')
      this.uploading = true
      request.post('/files/upload', formData)
        .then(result => {
          const path = (result && result.path) || (result && result.url) || ''
          if (!path) { this.$message.error('上传响应缺少文件路径'); return }
          this.form.avatar = path
          return patientApi.updateAvatar(path)
        })
        .then(result => {
          if (!result) return
          this.user = result
          setAuth({ token: getToken(), user: result })
          this.$message.success('头像已更新')
        })
        .catch(() => {})
        .finally(() => { this.uploading = false })
    }
  }
}
</script>
