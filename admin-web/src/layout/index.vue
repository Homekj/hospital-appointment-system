<template>
  <el-container class="shell">
    <div v-if="mobileMenuOpen" class="mobile-backdrop" @click="mobileMenuOpen = false" />
    <el-aside width="220px" class="aside" :class="{ 'aside--open': mobileMenuOpen }">
      <div class="brand">
        <div class="brand-logo">医</div>
        <div class="brand-text">
          <div class="brand-title">医院预约管理</div>
          <div class="brand-sub">{{ workspaceName }}</div>
        </div>
      </div>
      <el-menu
        class="side-menu"
        :default-active="$route.path"
        router
        background-color="#ffffff"
        text-color="#4b5563"
        active-text-color="#1d6fe0"
        @select="mobileMenuOpen = false"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <i :class="iconOf(item.icon)" />
          <span slot="title">{{ item.name }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">Hospital Appointment · v1.0</div>
    </el-aside>

    <el-container>
      <el-header class="header" height="64px">
        <div class="header-left">
          <button class="mobile-menu-toggle" type="button" aria-label="打开导航菜单" @click="mobileMenuOpen = true">
            <i class="el-icon-menu" />
          </button>
          <i class="el-icon-s-grid" />
          <span class="crumb">{{ workspaceName }}</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current">{{ $route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-tag size="small" :type="roleTagType" effect="plain">{{ ROLE_LABEL[role] || role || '未登录' }}</el-tag>
          <span class="user-name">{{ user.nickname || user.username || '用户' }}</span>
          <el-button type="text" icon="el-icon-switch-button" @click="logout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { dashboardApi, authApi } from '@/api/admin'
import { getUser, clearAuth, getRole, ROLE_LABEL } from '@/utils/auth'

// 后端 sys_menu.icon 存的是 Element Plus 图标名，这里映射到 Element UI 2 的字体图标
const ICON_MAP = {
  HomeFilled: 'el-icon-s-home',
  OfficeBuilding: 'el-icon-office-building',
  Avatar: 'el-icon-user',
  Calendar: 'el-icon-date',
  Tickets: 'el-icon-tickets',
  FirstAidKit: 'el-icon-first-aid-kit',
  User: 'el-icon-user-solid',
  Document: 'el-icon-document',
  Menu: 'el-icon-menu'
}

const MENUS = {
  ADMIN: [
    { name: '数据概览', path: '/admin/dashboard', icon: 'HomeFilled' },
    { name: '科室管理', path: '/admin/departments', icon: 'OfficeBuilding' },
    { name: '医生管理', path: '/admin/doctors', icon: 'Avatar' },
    { name: '排班管理', path: '/admin/schedules', icon: 'Calendar' },
    { name: '预约管理', path: '/admin/appointments', icon: 'Tickets' },
    { name: '处方与发药', path: '/admin/prescriptions', icon: 'FirstAidKit' },
    { name: '药品管理', path: '/admin/medicines', icon: 'FirstAidKit' },
    { name: '用户管理', path: '/admin/users', icon: 'User' },
    { name: '系统日志', path: '/admin/logs', icon: 'Document' },
    { name: '菜单管理', path: '/admin/menus', icon: 'Menu' }
  ],
  DOCTOR: [
    { name: '工作台概览', path: '/doctor/dashboard', icon: 'el-icon-s-home' },
    { name: '我的排班', path: '/doctor/schedules', icon: 'el-icon-date' },
    { name: '就诊列表', path: '/doctor/appointments', icon: 'el-icon-tickets' },
    { name: '接诊工作台', path: '/doctor/consultation', icon: 'el-icon-edit-outline' },
    { name: '药品目录', path: '/doctor/medicines', icon: 'el-icon-first-aid-kit' },
    { name: '个人资料', path: '/doctor/profile', icon: 'el-icon-user' }
  ],
  USER: [
    { name: '我的首页', path: '/patient/home', icon: 'el-icon-s-home' },
    { name: '预约挂号', path: '/patient/book', icon: 'el-icon-circle-plus' },
    { name: '我的预约', path: '/patient/appointments', icon: 'el-icon-tickets' },
    { name: '病历与处方', path: '/patient/clinical', icon: 'el-icon-document' },
    { name: '消息通知', path: '/patient/notifications', icon: 'el-icon-bell' },
    { name: '余额与充值', path: '/patient/wallet', icon: 'el-icon-wallet' },
    { name: '个人资料', path: '/patient/profile', icon: 'el-icon-user' }
  ]
}

const WORKSPACE = { ADMIN: 'ADMIN CONSOLE', DOCTOR: 'DOCTOR WORKBENCH', USER: 'PATIENT CENTER' }

export default {
  name: 'BasicLayout',
  data() {
    const role = getRole()
    return {
      ROLE_LABEL,
      role,
      menus: MENUS[role] || [],
      user: getUser(),
      mobileMenuOpen: false
    }
  },
  computed: {
    workspaceName() {
      return WORKSPACE[this.role] || 'HOSPITAL SYSTEM'
    },
    roleTagType() {
      return { ADMIN: 'danger', DOCTOR: 'primary', USER: 'success' }[this.role] || 'info'
    }
  },
  watch: {
    '$route'() {
      this.mobileMenuOpen = false
      this.syncRole()
    }
  },
  created() {
    this.syncRole()
    // /api/auth/me 对三个角色都开放，用于刷新昵称 / 余额 / 角色
    authApi.me().then(info => {
      if (info) this.user = info
    }).catch(() => {})
    // 只有管理员可以访问 /api/admin/menus，接口异常时回退本地菜单
    if (this.role === 'ADMIN') {
      dashboardApi.menus().then(list => {
        if (Array.isArray(list) && list.length) this.menus = list
      }).catch(() => {})
    }
  },
  methods: {
    syncRole() {
      const role = getRole()
      if (role !== this.role) {
        this.role = role
        this.menus = MENUS[role] || []
        this.user = getUser()
      }
    },
    iconOf(icon) {
      return ICON_MAP[icon] || (String(icon || '').startsWith('el-icon-') ? icon : 'el-icon-menu')
    },
    logout() {
      authApi.logout().catch(() => {}).finally(() => {
        clearAuth()
        this.$router.push('/login')
      })
    }
  }
}
</script>
