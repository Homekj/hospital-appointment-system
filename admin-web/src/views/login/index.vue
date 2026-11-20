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
        <el-form-item prop="captcha">
          <el-input v-model="form.captcha" prefix-icon="el-icon-key" placeholder="请输入右侧验证码" maxlength="4" autocomplete="off">
            <template slot="append">
              <button class="captcha-code" type="button" title="点击更换验证码" @click="refreshCaptcha">{{ captchaCode }}</button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登 录</el-button>
    </div>
  </div>
</template>

<script>
import { authApi } from '@/api/admin'
import { setAuth, clearAuth, roleHome } from '@/utils/auth'

export default {
  name: 'LoginPage',
  data() {
    return {
      loading: false,
      form: { username: '', password: '', captcha: '' },
      captchaCode: '',
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
      }
    }
  },
  created() { this.refreshCaptcha() },
  methods: {
    refreshCaptcha() {
      const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
      this.captchaCode = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.captcha.trim().toUpperCase() !== this.captchaCode) {
          this.$message.error('验证码不正确')
          this.form.captcha = ''
          this.refreshCaptcha()
          return
        }
        this.loading = true
        authApi.login({ username: this.form.username, password: this.form.password })
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
          .finally(() => { this.loading = false; this.form.captcha = ''; this.refreshCaptcha() })
      })
    },
    roleLabel(role) {
      return { ADMIN: '管理员', DOCTOR: '医生', USER: '患者' }[role] || ''
    }
  }
}
</script>

<style scoped>
.captcha-code { border: 0; background: transparent; color: #1d6fe0; font-size: 18px; font-weight: 700; letter-spacing: 4px; cursor: pointer; }
</style>
