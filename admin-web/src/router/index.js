import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/index.vue'
import { getToken, getRole, roleHome } from '@/utils/auth'

Vue.use(VueRouter)

const ADMIN = ['ADMIN']
const DOCTOR = ['DOCTOR']
const USER = ['USER']

const adminChildren = [
  { path: '/admin/dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据概览', icon: 'el-icon-s-home', roles: ADMIN } },
  { path: '/admin/departments', name: 'Departments', component: () => import('@/views/departments/index.vue'), meta: { title: '科室管理', icon: 'el-icon-office-building', roles: ADMIN } },
  { path: '/admin/doctors', name: 'Doctors', component: () => import('@/views/doctors/index.vue'), meta: { title: '医生管理', icon: 'el-icon-user', roles: ADMIN } },
  { path: '/admin/schedules', name: 'Schedules', component: () => import('@/views/schedules/index.vue'), meta: { title: '排班管理', icon: 'el-icon-date', roles: ADMIN } },
  { path: '/admin/appointments', name: 'Appointments', component: () => import('@/views/appointments/index.vue'), meta: { title: '预约管理', icon: 'el-icon-tickets', roles: ADMIN } },
  { path: '/admin/medicines', name: 'Medicines', component: () => import('@/views/medicines/index.vue'), meta: { title: '药品管理', icon: 'el-icon-first-aid-kit', roles: ADMIN } },
  { path: '/admin/users', name: 'Users', component: () => import('@/views/users/index.vue'), meta: { title: '用户管理', icon: 'el-icon-user-solid', roles: ADMIN } },
  { path: '/admin/logs', name: 'Logs', component: () => import('@/views/logs/index.vue'), meta: { title: '系统日志', icon: 'el-icon-document', roles: ADMIN } },
  { path: '/admin/menus', name: 'Menus', component: () => import('@/views/menus/index.vue'), meta: { title: '菜单管理', icon: 'el-icon-menu', roles: ADMIN } }
]

const doctorChildren = [
  { path: '/doctor/dashboard', name: 'DoctorDashboard', component: () => import('@/views/doctor/dashboard.vue'), meta: { title: '工作台概览', icon: 'el-icon-s-home', roles: DOCTOR } },
  { path: '/doctor/schedules', name: 'DoctorSchedules', component: () => import('@/views/doctor/schedules.vue'), meta: { title: '我的排班', icon: 'el-icon-date', roles: DOCTOR } },
  { path: '/doctor/appointments', name: 'DoctorAppointments', component: () => import('@/views/doctor/appointments.vue'), meta: { title: '就诊列表', icon: 'el-icon-tickets', roles: DOCTOR } },
  { path: '/doctor/consultation', name: 'DoctorConsultation', component: () => import('@/views/doctor/consultation.vue'), meta: { title: '接诊工作台', icon: 'el-icon-edit-outline', roles: DOCTOR } },
  { path: '/doctor/medicines', name: 'DoctorMedicines', component: () => import('@/views/doctor/medicines.vue'), meta: { title: '药品目录', icon: 'el-icon-first-aid-kit', roles: DOCTOR } },
  { path: '/doctor/profile', name: 'DoctorProfile', component: () => import('@/views/doctor/profile.vue'), meta: { title: '个人资料', icon: 'el-icon-user', roles: DOCTOR } }
]

const patientChildren = [
  { path: '/patient/home', name: 'PatientHome', component: () => import('@/views/patient/home.vue'), meta: { title: '我的首页', icon: 'el-icon-s-home', roles: USER } },
  { path: '/patient/book', name: 'PatientBook', component: () => import('@/views/patient/book.vue'), meta: { title: '预约挂号', icon: 'el-icon-circle-plus', roles: USER } },
  { path: '/patient/appointments', name: 'PatientAppointments', component: () => import('@/views/patient/appointments.vue'), meta: { title: '我的预约', icon: 'el-icon-tickets', roles: USER } },
  { path: '/patient/clinical', name: 'PatientClinical', component: () => import('@/views/patient/clinical.vue'), meta: { title: '病历与处方', icon: 'el-icon-document', roles: USER } },
  { path: '/patient/notifications', name: 'PatientNotifications', component: () => import('@/views/patient/notifications.vue'), meta: { title: '消息通知', icon: 'el-icon-bell', roles: USER } },
  { path: '/patient/wallet', name: 'PatientWallet', component: () => import('@/views/patient/wallet.vue'), meta: { title: '余额与充值', icon: 'el-icon-wallet', roles: USER } },
  { path: '/patient/profile', name: 'PatientProfile', component: () => import('@/views/patient/profile.vue'), meta: { title: '个人资料', icon: 'el-icon-user', roles: USER } }
]

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: Layout,
    redirect: () => roleHome(getRole()),
    children: [...adminChildren, ...doctorChildren, ...patientChildren]
  },
  { path: '*', redirect: () => roleHome(getRole()) }
]

const router = new VueRouter({ mode: 'hash', routes })

router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '医院预约挂号管理系统'
  if (to.meta.public) {
    next()
    return
  }
  if (!getToken()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  const role = getRole()
  if (to.meta.roles && to.meta.roles.indexOf(role) === -1) {
    // 越权访问其它角色页面时，回到本角色首页，而不是弹 403
    next({ path: roleHome(role), replace: true })
    return
  }
  next()
})

export default router
