# 医院预约系统前端（Vue 2 + Element UI）

技术栈：Vue 2.7、Element UI 2.15、vue-router 3、axios 1、Vite 5（`@vitejs/plugin-vue2`）。

**一个登录入口、三套工作台**：登录接口 `POST /api/auth/login` 对所有角色开放，前端按返回的 `data.user.role` 自动分流到管理员后台 / 医生工作台 / 患者中心。

## 启动

```bash
cp .env.example .env
npm install
npm run dev          # http://localhost:5173
```

生产构建：`npm run build`（产物 `dist/`），预览：`npm run preview`。

## 登录说明

登录页不预填或展示演示账号，请手动输入已创建的账号和密码，并完成页面上的随机验证码校验。小程序患者可使用微信登录；需要网页登录时，患者需先设置网页登录密码。

## 环境变量

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_BACKEND_URL` | `http://localhost:9090` | Vite 开发/预览代理的目标后端 |
| `VITE_API_BASE_URL` | `/api` | axios 请求前缀；生产分离部署时可填完整地址 |

开发时 `/api`、`/admin`、`/uploads` 都走代理，浏览器不产生跨域请求。

## 目录

```
src/
├── api/admin.js          # 管理员接口（含旧 /admin/** 删除接口）
├── api/doctor.js         # 医生工作台 /api/doctor/**
├── api/patient.js        # 患者 /api/appointments、/notifications、/wallet 等
├── utils/request.js      # axios：Bearer token、统一 code 判定、401 跳登录
├── utils/auth.js         # 登录态、角色常量与 roleHome 分流
├── utils/format.js       # 时间容错格式化、预约状态字典、支付方式
├── router/index.js       # 三角色路由 + meta.roles 守卫（hash 模式）
├── layout/index.vue      # 按角色切换侧边菜单 + 顶栏
├── mixins/listPage.js    # 列表页通用分页 / 查询 / 弹窗 / 删除逻辑
├── views/
│   ├── login/            # 统一登录页
│   ├── dashboard|departments|doctors|schedules|appointments|medicines|users|logs|menus/
│   │                     # 管理员后台 9 页
│   ├── doctor/           # 概览、我的排班、就诊列表、接诊工作台、药品目录
│   └── patient/          # 首页、预约挂号、我的预约、病历处方、消息、余额、资料
└── styles/index.css
```

## 已实现的功能

**通用**

- 登录保存 JWT，请求自动带 `Authorization: Bearer <token>`；401 清理登录态并跳登录；路由守卫按 `meta.roles` 阻止跨角色访问，越权时回到本角色首页。
- 所有列表页把 `current/size` 与筛选条件发给后端，`el-pagination` 绑定后端 `total`，不做前端假分页。

**管理员（9 页）**：科室 / 医生 / 排班 / 药品完整 CRUD + 启停用；预约详情、取消、签到、状态流转、CSV 导出；用户启停用；登录与操作日志；菜单只读视图；数据概览与近 7 日趋势。

**医生（5 页）**：概览统计（对接 `/api/doctor/dashboard`）、我的排班、就诊列表分页；接诊工作台按状态机依次完成「开始接诊 → 保存病历 → 完成接诊 / 开处方」，处方药品从 `/api/doctor/medicines` 选择，单价由后端计算；药品目录查询。

**患者（7 页）**：首页聚合余额 / 预约 / 未读消息并含智能分诊；预约挂号走「科室 → 医生 → 号源 → 就诊人 → 支付」完整链路；我的预约支持支付、取消、签到、评价；病历与处方含明细和药费支付；消息通知支持单条/全部已读；余额充值与流水；个人资料含头像上传。

## 注意事项

- 后端权限边界在 `RoleAccessPolicy`：ADMIN 只能访问 `/api/admin/**` 与 `/admin/**`，DOCTOR 只能访问 `/api/doctor/**`，USER 只能访问 `/api/appointments`、`/api/notifications`、`/api/wallet`、`/api/prescriptions`；`/api/auth/me`、`/api/profile`、`/api/files/upload` 三角色共用。前端分流只是展示层，不替代后端鉴权。
- 删除接口后端只保留在旧 `/admin/**`，`api/admin.js` 用 `baseURL: ROOT` 单独指向后端根路径。
- 新版接口失败时 HTTP 状态码等于业务码；旧删除接口失败时是 HTTP 200 + `code = 500`。`request.js` 对两者统一按 `code` 判断。
- 预约状态严格按 `sql/system.sql` 的七个值；「待取药」是患者列表的派生筛选值 `pending_dispense`。
- 后端部分 `LocalDateTime` 字段在不同 Jackson 版本下可能是 ISO 字符串或数组，`utils/format.js` 已做统一容错。
- 后台「系统日志 → 操作日志」由后端 AOP 切面自动记录所有写请求（模块、动作、请求方式、URL、IP、耗时、成功/失败），登录日志则写入另一张表；页面只负责分页查询与筛选，不做任何日志写入。
