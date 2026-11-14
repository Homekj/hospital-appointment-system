<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <div class="brand-logo">医</div>
        <div>
          <div class="login-title">医院预约挂号管理系统</div>
          <div class="login-sub">ADMIN / DOCTOR / PATIENT</div>
        </div>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 18px"
        title="同一账号体系按角色进入工作台；小程序患者可在“我的”中设置网页登录密码" />

      <el-form ref="form" :model="form" :rules="rules" @keyup.enter.native="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" prefix-icon="el-icon-user" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" prefix-icon="el-icon-lock" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
      </el-form>

      <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登 录</el-button>

      <div class="demo-box">
        <div class="demo-title">演示账号（点击自动填入）</div>
        <div class="demo-buttons">
          <el-button size="mini" icon="el-icon-s-custom" @click="fill('admin', 'admin')">管理员</el-button>
          <el-button size="mini" icon="el-icon-user" @click="fill('doctorzmh', '123')">医生</el-button>
          <el-button size="mini" icon="el-icon-s-check" @click="fill(PATIENT_ACCOUNT, '123')">患者</el-button>
        </div>
        <div class="demo-hint">
          管理员：admin / admin　·　医生：doctorzmh / 123<br />
          患者：{{ PATIENT_ACCOUNT }} / 123（演示数据绑定此账号）
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { authApi } from '@/api/admin'
import { setAuth, clearAuth, roleHome } from '@/utils/auth'

const PATIENT_ACCOUNT = 'wx_ogHVa5P9lsJ4o0rAE2EKJeCD-OAw'

export default {
  name: 'LoginPage',
  data() {
    return {
      PATIENT_ACCOUNT,
      loading: false,
      form: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    fill(username, password) {
      this.form = { username, password }
      this.$nextTick(() => { if (this.$refs.form) this.$refs.form.clearValidate() })
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        authApi.login(this.form)
          .then(data => {
            if (!data || !data.token) {
              this.$message.error('登录响应缺少令牌')
              return
            }
            const role = (data.user && data.user.role || '').toUpperCase()
            if (!['ADMIN', 'DOCTOR', 'USER'].includes(role)) {
              clearAuth()
              this.$message.error('账号角色未配置，无法登录')
              return
            }
            setAuth(data)
            this.$message.success(`登录成功，正在进入${this.roleLabel(role)}工作台`)
            const redirect = this.$route.query.redirect
            // 越权的 redirect 会由路由守卫自动纠正到本角色首页
            this.$router.replace(redirect || roleHome(role))
          })
          .catch(() => {})
          .finally(() => { this.loading = false })
      })
    },
    roleLabel(role) {
      return { ADMIN: '管理员', DOCTOR: '医生', USER: '患者' }[role] || ''
    }
  }
}
</script>

<style scoped>
.demo-box { margin-top: 20px; padding-top: 16px; border-top: 1px solid #eef1f6; }
.demo-title { font-size: 12px; color: #8a93a6; margin-bottom: 10px; }
.demo-buttons { display: flex; gap: 8px; justify-content: center; }
.demo-hint { font-size: 11px; color: #a6afbf; text-align: center; margin-top: 12px; line-height: 1.8; word-break: break-all; }
</style>
