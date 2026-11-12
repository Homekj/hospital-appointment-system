# 医院预约管理系统 REST API 契约

> 版本：`1.0`
>
> 更新时间：`2026-11-09`
>
> 适用仓库：[StreamH/Hospital-Appointment-Management](https://gitee.com/StreamH/Hospital-Appointment-Management)（Gitee）
>
> 适用范围：微信患者端小程序、Vue 管理后台、Spring Boot 后端、医生工作台

本文档是前后端并行开发的接口唯一事实源。接口字段、状态值、权限或错误语义发生变化时，必须先修改本文档，再修改后端实现并通知联调人员。

## 1. 接口范围

本系统包含三个客户端：

| 客户端 | 使用角色 | 主要能力 |
|---|---|---|
| 微信小程序 | `USER` | 注册登录、查科室和医生、选号、预约、模拟支付、签到、查看病历和处方、评价、消息、余额充值 |
| 管理后台 | `ADMIN` | 科室、医生、排班、预约、药品、用户、日志、处方发药和数据概览 |
| 医生工作台 | `DOCTOR` | 查看本人排班和预约、叫号接诊、填写病历、完成接诊、开具处方 |

所有客户端只访问 Spring Boot 提供的 `/api` 接口，不得直接连接 MySQL 或 Redis。

## 2. 环境与地址

### 2.1 开发环境

| 服务 | 地址 |
|---|---|
| Spring Boot API | `http://localhost:9090/api` |
| 微信小程序请求前缀 | `http://localhost:9090` |
| 管理后台开发地址 | `http://localhost:5173` |
| 局域网管理后台示例 | `http://192.168.0.48:5173` |
| MySQL | `localhost:3306/hospital_booking` |
| Redis | `localhost:6379` |

微信开发者工具开发时勾选“不校验合法域名”。真机调试时，将小程序 `utils/config.js` 中的 `baseUrl` 改为电脑局域网 IP，并确保手机与电脑在同一网络。

前端不得写死数据库密码、JWT 密钥、微信 `AppSecret`、真实令牌或其他敏感信息。

### 2.2 请求格式

- 除文件上传外，请求和响应均使用 `application/json; charset=UTF-8`。
- JSON 字段使用 `camelCase`。
- 数据库 `snake_case` 字段由后端转换为前端使用的 `camelCase` 字段。
- 不在 URL 或日志中传递密码、身份证号、令牌等敏感数据。

## 3. 通用响应

### 3.1 统一响应结构

所有业务接口统一返回：

```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | integer | 业务结果码，成功固定为 `200` |
| `data` | any | 业务数据；无返回数据时为 `null` |
| `message` | string | 成功固定为 `success`；失败时为可直接展示的中文信息 |

小程序现有请求封装要求 HTTP 响应体中的 `code === 200`，否则会按失败处理。后端不得仅依赖 HTTP 状态码而省略统一响应体。

### 3.2 分页响应

分页接口的 `data` 固定为：

```json
{
  "records": [],
  "total": 0,
  "current": 1,
  "size": 10
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `records` | array | 当前页记录 |
| `total` | integer | 符合条件的总记录数 |
| `current` | integer | 当前页码，从 `1` 开始 |
| `size` | integer | 每页条数 |

通用分页参数：

| 参数 | 类型 | 默认值 | 约束 |
|---|---:|---:|---|
| `current` | integer | `1` | 最小 `1` |
| `size` | integer | `10` | `1` 到 `100` |

### 3.3 HTTP 状态码与业务码

| HTTP 状态码 | `code` | 使用场景 |
|---:|---:|---|
| `200` | `200` | 成功 |
| `400` | `400` | 参数缺失、格式错误、状态不允许 |
| `401` | `401` | 未登录、令牌无效或已过期 |
| `403` | `403` | 已登录但角色或数据归属不匹配 |
| `404` | `404` | 资源不存在 |
| `409` | `409` | 重复预约、重复评价、号源已满、并发冲突 |
| `500` | `500` | 未处理的服务端错误 |

错误响应示例：

```json
{
  "code": 409,
  "data": null,
  "message": "该时段号源已满"
}
```

后端不得向前端返回 Java 堆栈、SQL、Redis 地址、内部类名或密钥。

### 3.4 字段类型约定

| 类型 | 约定 |
|---|---|
| `id` | JSON 数字，对应 Java `Long`，当前演示数据量不会超过 JavaScript 安全整数范围 |
| 金额 | JSON 数字，保留两位小数，例如 `20.00` |
| 日期 | `yyyy-MM-dd`，例如 `2026-11-09` |
| 日期时间 | `yyyy-MM-dd HH:mm:ss`，例如 `2026-11-09 15:30:00` |
| 布尔值 | `true` / `false` |
| 可空字段 | 返回 `null`，不返回空字符串冒充无值 |

## 4. 认证与权限

### 4.1 JWT 规则

除公开接口外，请求统一携带：

```http
Authorization: Bearer <JWT>
```

JWT 最少包含以下声明：

| 声明 | 说明 |
|---|---|
| `sub` | 用户 ID |
| `role` | `ADMIN`、`DOCTOR` 或 `USER` |
| `iat` | 签发时间 |
| `exp` | 过期时间 |

JWT 不保存密码、手机号、身份证号等敏感信息。JWT 密钥只能通过环境变量或外部配置注入。

### 4.2 角色

| 角色编码 | 名称 | 说明 |
|---|---|---|
| `ADMIN` | 管理员 | 管理后台全部业务 |
| `DOCTOR` | 医生 | 仅访问本人绑定医生 ID 的数据 |
| `USER` | 患者 | 仅访问本人预约、处方、钱包和通知 |

### 4.3 数据和权限边界

- `USER` 只能读写 `user_id` 等于当前登录用户 ID 的数据。
- `DOCTOR` 账号必须通过 `sys_user.doctor_id` 绑定唯一医生。
- `DOCTOR` 只能查看和操作本人排班、预约、病历和处方。
- `ADMIN` 可以查看全量管理数据，但不通过患者专属接口绕过数据校验。
- 禁用用户不得登录，已有令牌应在后续请求中拒绝。
- 客户端传入的用户 ID 不参与数据归属判断，后端必须从 JWT 获取当前用户。

### 4.4 接口权限矩阵

| 接口范围 | 公开 | `USER` | `DOCTOR` | `ADMIN` |
|---|---:|---:|---:|---:|
| `/api/auth/login` | 是 | 是 | 是 | 是 |
| `/api/auth/wx-login` | 是 | 是 | 否 | 否 |
| `/api/departments`、`/api/doctors`、`/api/schedules` | 是 | 是 | 是 | 是 |
| `/api/triage/analyze` | 是 | 是 | 是 | 是 |
| `/api/auth/me`、`/api/profile/**`、`/api/files/upload` | 否 | 是 | 是 | 是 |
| `/api/appointments/**` | 否 | 是 | 否 | 否 |
| `/api/prescriptions/**` | 否 | 是 | 否 | 否 |
| `/api/notifications/**`、`/api/wallet/**` | 否 | 是 | 否 | 否 |
| `/api/admin/**` | 否 | 否 | 否 | 是 |
| `/api/doctor/**` | 否 | 否 | 是 | 否 |

## 5. 枚举和状态机

### 5.1 预约状态

数据库真实值：

| 状态 | 含义 | 可执行操作 |
|---|---|---|
| `unpaid` | 已占号，待支付 | 支付、取消；超时自动取消 |
| `pending` | 已支付，待就诊 | 到院签到、取消 |
| `checked_in` | 已签到，等待医生接诊 | 医生开始接诊 |
| `in_consultation` | 就诊中 | 医生填写病历、完成接诊 |
| `pending_rx_pay` | 已开处方，待缴药费 | 患者支付处方；无可支付处方时可由医生或管理员结束 |
| `completed` | 已完成 | 患者评价、查看病历和处方 |
| `cancelled` | 已取消 | 只读 |

接口兼容说明：

- `confirmed` 是旧前端代码中的历史别名，等同于 `pending`。
- 新后端只写入 `pending`，不得再产生 `confirmed`。
- 小程序展示状态 `pending_dispense` 不是数据库状态。当预约状态为 `pending_rx_pay` 且处方状态为 `paid` 时，接口可按查询条件将其筛选并返回原预约状态 `pending_rx_pay`，前端自行展示为“待取药”。

预约状态流转：

```text
unpaid --支付成功--> pending --患者签到--> checked_in
checked_in --医生接诊--> in_consultation
in_consultation --开处方--> pending_rx_pay --处方发药--> completed
in_consultation --无处方完成--> completed

unpaid --用户取消/超时--> cancelled
pending --用户或管理员取消--> cancelled
```

禁止的流转示例：

- `unpaid` 不能直接签到。
- `completed` 或 `cancelled` 不能再次支付、取消或接诊。
- 非本人预约不能被患者支付、取消、签到或评价。
- 非接诊医生不能开始或完成对应预约。

### 5.2 处方状态

| 状态 | 含义 | 允许操作 |
|---|---|---|
| `unpaid` | 待支付 | 患者支付、管理员取消 |
| `paid` | 已支付，待发药 | 管理员发药 |
| `dispensed` | 已发药 | 只读 |
| `cancelled` | 已取消 | 只读 |

处方流转：

```text
unpaid --患者支付--> paid --管理员发药--> dispensed
unpaid --管理员取消--> cancelled
```

处方支付后，预约仍保持 `pending_rx_pay`；管理员发药成功后，将处方更新为 `dispensed`，同时将预约更新为 `completed`。

### 5.3 其他枚举

支付方式：

| 值 | 含义 |
|---|---|
| `wechat` | 微信模拟支付 |
| `alipay` | 支付宝模拟支付 |
| `balance` | 账户余额支付 |

通用启用状态：

| 值 | 含义 |
|---|---|
| `1` | 启用或正常 |
| `0` | 停用、停诊或禁用 |

性别：

| 值 | 含义 |
|---|---|
| `1` | 男 |
| `2` | 女 |

通知业务类型：

| 值 | 含义 |
|---|---|
| `appointment` | 预约业务 |
| `prescription` | 处方业务 |

余额流水类型：

| 值 | 含义 |
|---|---|
| `recharge` | 充值 |
| `order_pay` | 预约或处方支付 |
| `order_refund` | 预约退款 |

## 6. 认证接口

### 6.1 账号密码登录

`POST /api/auth/login`

权限：公开

请求：

```json
{
  "username": "example_user",
  "password": "example_password"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `username` | string | 是 | 1 到 50 字符 |
| `password` | string | 是 | 1 到 72 字符 |

成功响应：

```json
{
  "code": 200,
  "data": {
    "token": "<JWT>",
    "user": {
      "id": 1,
      "username": "example_admin",
      "nickname": "系统管理员",
      "email": "admin@example.com",
      "phone": "13800000000",
      "avatar": null,
      "balance": 0.00,
      "role": "ADMIN",
      "doctorId": null,
      "status": 1
    }
  },
  "message": "success"
}
```

失败场景：

| 条件 | 响应 |
|---|---|
| 用户名或密码错误 | `401`，`用户名或密码错误` |
| 用户被禁用 | `403`，`账号已禁用` |
| 连续登录失败次数过多 | `400` 或 `409`，提示稍后重试 |

### 6.2 微信登录

`POST /api/auth/wx-login`

权限：公开

请求：

```json
{
  "code": "<wx.login 返回的一次性 code>",
  "nickname": "微信昵称"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `code` | string | 是 | 微信一次性登录凭证 |
| `nickname` | string | 否 | 最大 50 字符 |

处理规则：

- 后端使用 `code` 换取微信 `openid`。
- `openid` 只保存在服务端，不在接口响应中返回。
- 首次登录自动创建 `USER` 账号。
- 之后按 `openid` 查找并登录已有用户。
- 不得把 `AppSecret` 写入仓库或返回客户端。
- 模拟登录默认关闭。仅在本地演示时显式设置 `WECHAT_MINIPROGRAM_MOCK_LOGIN=true`，启用固定演示患者登录且不调用微信换码接口；真实或部署环境应保持关闭并配置 AppID / AppSecret。

成功响应与账号密码登录相同，`user.role` 固定为 `USER`。

### 6.3 当前用户

`GET /api/auth/me`

权限：已登录

成功响应 `data` 为用户对象：

```json
{
  "id": 2,
  "username": "example_user",
  "nickname": "演示患者",
  "email": null,
  "phone": "13900000000",
  "avatar": "/uploads/avatar/example.jpg",
  "balance": 100.00,
  "role": "USER",
  "doctorId": null,
  "status": 1
}
```

响应中禁止包含 `password`、`openid`、`deleted`。

### 6.4 退出登录

`POST /api/auth/logout`

权限：已登录

成功响应：

```json
{
  "code": 200,
  "data": null,
  "message": "success"
}
```

如启用 JWT 黑名单，应将当前令牌加入 Redis 黑名单直至其自然过期；Redis 故障时不得阻断本地客户端清除令牌。

### 6.5 修改个人资料

`PUT /api/profile`

权限：已登录

请求：

```json
{
  "nickname": "新的昵称",
  "email": "user@example.com",
  "phone": "13900000000"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `nickname` | string | 是 | 1 到 50 字符 |
| `email` | string | 否 | 最大 100 字符，格式合法 |
| `phone` | string | 否 | 最大 20 字符 |

成功响应 `data` 为更新后的用户对象。

### 6.6 绑定头像

`PUT /api/profile/avatar`

权限：已登录

请求：

```json
{
  "path": "/uploads/avatar/example.jpg"
}
```

`path` 必须来自文件上传接口返回结果，不能接受任意远程 URL。成功响应 `data` 为更新后的用户对象。

### 6.7 上传文件

`POST /api/files/upload`

权限：已登录

请求类型：`multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `file` | file | 是 | 图片文件 |
| `category` | string | 否 | 当前支持 `avatar` |

限制：

- 仅允许 `jpg`、`jpeg`、`png`、`webp`。
- 单文件不超过 5 MB。
- 服务端生成随机文件名，禁止直接使用客户端文件名作为磁盘路径。

成功响应：

```json
{
  "code": 200,
  "data": {
    "path": "/uploads/avatar/20261109-example.jpg",
    "url": "/uploads/avatar/20261109-example.jpg"
  },
  "message": "success"
}
```

## 7. 公共查询与智能分诊

### 7.1 科室列表

`GET /api/departments`

权限：公开

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `keyword` | string | 否 | 按科室名称模糊查询 |

该接口返回数组，不分页。

成功响应 `data`：

```json
[
  {
    "id": 1,
    "name": "内科",
    "description": "呼吸、消化、内分泌等",
    "location": "门诊楼3楼",
    "sortOrder": 1,
    "status": 1
  }
]
```

默认只返回 `status = 1` 的科室，并按 `sortOrder` 升序、`id` 升序排列。

### 7.2 医生列表

`GET /api/doctors`

权限：公开

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `departmentId` | integer | 否 | 科室 ID |
| `keyword` | string | 否 | 医生姓名或职称模糊查询 |
| `hasAvailableSlot` | boolean | 否 | `true` 时只返回未来 7 天内存在剩余号源的医生 |

该接口返回数组，不分页，保持小程序现有调用方式。

成功响应 `data`：

```json
[
  {
    "id": 1,
    "departmentId": 1,
    "departmentName": "内科",
    "name": "张明华",
    "title": "主任医师",
    "gender": 1,
    "avatar": "/uploads/common/doctor_m_1.jpg",
    "intro": "擅长呼吸系统疾病诊治，从业20年",
    "rating": 4.9,
    "reviewCount": 12,
    "consultCount": 36,
    "hasAvailableSlot": true,
    "status": 1
  }
]
```

字段来源说明：

| 字段 | 来源 |
|---|---|
| `rating` | `appointment_review.rating` 聚合值；无评价时为 `null` |
| `reviewCount` | 医生评价数量 |
| `consultCount` | 医生已完成预约数量 |
| `hasAvailableSlot` | 未来 7 天存在 `status = 1` 且 `booked_count < total_quota` 的排班 |

### 7.3 医生详情

`GET /api/doctors/{id}`

权限：公开

成功响应的 `data` 为医生对象。医生不存在或已停用时返回 `404`。

### 7.4 排班号源

`GET /api/schedules`

权限：公开

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `doctorId` | integer | 否 | 医生 ID |
| `scheduleDate` | string | 否 | 日期，格式 `yyyy-MM-dd` |
| `startDate` | string | 否 | 查询开始日期 |
| `endDate` | string | 否 | 查询结束日期 |

该接口返回数组，不分页。

成功响应 `data`：

```json
[
  {
    "id": 101,
    "doctorId": 1,
    "scheduleDate": "2026-11-09",
    "timeSlot": "09:00",
    "totalQuota": 5,
    "bookedCount": 2,
    "remainQuota": 3,
    "status": 1
  }
]
```

公开接口默认只返回今天及以后、`status = 1` 的排班。`remainQuota = totalQuota - bookedCount`，最小为 `0`。

### 7.5 智能分诊

`POST /api/triage/analyze`

权限：公开

该接口只用于科室推荐，不构成诊断或医疗建议。

请求：

```json
{
  "symptom": "发热、咳嗽、咽痛"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `symptom` | string | 是 | 1 到 200 字符 |

成功响应：

```json
{
  "code": 200,
  "data": {
    "summary": "症状以发热、咳嗽和咽痛为主",
    "symptomAnalysis": "建议优先排查呼吸道感染",
    "commonSigns": ["发热", "咳嗽", "咽痛"],
    "preliminaryConclusion": "可优先考虑呼吸内科或感染相关科室",
    "advice": "若持续高热、呼吸困难或意识异常，请立即线下就医",
    "urgent": false,
    "source": "fallback",
    "departments": [
      {
        "id": 1,
        "name": "内科",
        "description": "呼吸、消化、内分泌等",
        "location": "门诊楼3楼",
        "sortOrder": 1,
        "status": 1,
        "score": 5,
        "symptoms": ["发热", "咳嗽", "咽痛"]
      }
    ]
  },
  "message": "success"
}
```

`source` 取值：

| 值 | 含义 |
|---|---|
| `deepseek` | 外部 AI 分析成功 |
| `fallback` | 使用本地关键词规则 |

外部 AI 不可用、超时或返回格式错误时必须降级到本地规则，不得导致分诊接口整体不可用。

## 8. 患者预约接口

### 8.1 我的预约列表

`GET /api/appointments`

权限：`USER`

只返回当前登录用户的数据。

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `current` | integer | 否 | 默认 `1` |
| `size` | integer | 否 | 默认 `10`，最大 `100` |
| `status` | string | 否 | 预约状态 |

支持的筛选状态：

- `unpaid`
- `pending`
- `checked_in`
- `in_consultation`
- `pending_rx_pay`
- `pending_dispense`
- `completed`
- `cancelled`

`pending_dispense` 是派生筛选：预约状态为 `pending_rx_pay`，且关联处方状态为 `paid`。

`pending` 筛选应同时兼容历史数据中的 `confirmed`。

成功响应为分页对象，`records` 中每一项使用预约对象。

### 8.2 预约详情

`GET /api/appointments/{id}`

权限：`USER`，且预约必须属于当前用户

成功响应 `data`：

```json
{
  "id": 1001,
  "userId": 2,
  "scheduleId": 101,
  "doctorId": 1,
  "doctorName": "张明华",
  "doctorTitle": "主任医师",
  "doctorAvatar": "/uploads/common/doctor_m_1.jpg",
  "departmentId": 1,
  "departmentName": "内科",
  "departmentLocation": "门诊楼3楼",
  "scheduleDate": "2026-11-09",
  "timeSlot": "09:00",
  "patientName": "演示患者",
  "patientPhone": "13900000000",
  "idCard": null,
  "amount": 20.00,
  "payMethod": "balance",
  "payMethodText": "余额支付",
  "payTime": "2026-11-09 09:10:00",
  "status": "pending",
  "remark": "咳嗽三天",
  "prescriptionId": null,
  "prescriptionStatus": null,
  "prescriptionAmount": 0.00,
  "hasConsultation": false,
  "evaluated": false,
  "createTime": "2026-11-09 09:08:00",
  "updateTime": "2026-11-09 09:10:00"
}
```

`idCard` 只在明确需要展示时返回；列表接口默认脱敏或不返回身份证号。

### 8.3 创建预约

`POST /api/appointments`

权限：`USER`

请求：

```json
{
  "scheduleId": 101,
  "patientName": "演示患者",
  "patientPhone": "13900000000",
  "idCard": null,
  "remark": "咳嗽三天"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `scheduleId` | integer | 是 | 必须存在且可预约 |
| `patientName` | string | 是 | 1 到 50 字符 |
| `patientPhone` | string | 是 | 1 到 20 字符，建议校验中国大陆手机号 |
| `idCard` | string | 否 | 最大 30 字符 |
| `remark` | string | 否 | 最大 200 字符 |

创建成功返回 HTTP `200`，`data` 为预约对象，初始状态必须为 `unpaid`。

核心事务：

1. 校验排班存在、状态正常且未就诊日期已过。
2. 使用条件更新扣减号源：

```sql
UPDATE schedule
SET booked_count = booked_count + 1
WHERE id = ?
  AND status = 1
  AND booked_count < total_quota;
```

3. 影响行数为 `0` 时返回 `409`，消息为“该时段号源已满”。
4. 扣减成功后在同一 MySQL 事务内插入 `appointment`。
5. 事务失败时号源和预约必须同时回滚。

并发和幂等：

- 必须使用 MySQL 条件更新防止超卖，Redis 锁只能作为辅助。
- 同一用户对同一 `scheduleId` 已存在 `unpaid`、`pending`、`checked_in` 或 `in_consultation` 预约时，默认拒绝重复创建并返回 `409`。
- 如接入 `Idempotency-Key` 请求头，同一用户同一键的重复请求应返回首次创建结果。

### 8.4 支付挂号费

`POST /api/appointments/{id}/pay`

权限：`USER`，且预约属于当前用户

请求：

```json
{
  "payMethod": "balance"
}
```

| 字段 | 类型 | 必填 | 允许值 |
|---|---|---:|---|
| `payMethod` | string | 是 | `wechat`、`alipay`、`balance` |

成功响应 `data` 为支付后的预约对象，状态应为 `pending`。

事务规则：

- 仅 `unpaid` 可支付。
- 使用条件更新：`WHERE id = ? AND user_id = ? AND status = 'unpaid'`。
- `balance` 支付必须校验余额，并原子扣减 `sys_user.balance`、写入 `balance_log`、更新预约状态。
- `wechat` 和 `alipay` 当前为模拟支付，不调用真实支付网关；接口成功后视为支付成功。
- 相同预约已支付时，如支付方式相同可返回当前预约并保持幂等；状态不可支付时返回 `400`。
- 余额不足返回 `409`，消息为“余额不足，请先充值”。

### 8.5 取消预约

`PUT /api/appointments/{id}/cancel`

权限：`USER`，且预约属于当前用户

请求体为空。

允许取消状态：

- `unpaid`
- `pending`

成功响应 `data` 为取消后的预约对象，状态为 `cancelled`。

事务规则：

- 使用条件更新防止重复取消。
- 取消成功后必须将对应排班 `booked_count` 减 `1`，且不得小于 `0`。
- `unpaid` 取消不涉及退款。
- `balance` 支付的预约取消时，挂号费原路退回余额并写入 `order_refund` 流水。
- `wechat`、`alipay` 为模拟支付，记录模拟退款说明，不调用真实退款网关。
- `checked_in`、`in_consultation`、`pending_rx_pay`、`completed`、`cancelled` 不允许患者自行取消。

### 8.6 到院签到

`PUT /api/appointments/{id}/check-in`

权限：`USER`，且预约属于当前用户

请求体为空。

规则：

- 仅 `pending` 可签到。
- 默认只允许就诊当天签到。
- 条件更新状态为 `checked_in`。
- 重复签到返回当前预约或 `409`，不得导致状态倒退。

成功响应 `data` 为签到后的预约对象。

### 8.7 查看病历

`GET /api/appointments/{id}/consultation`

权限：`USER`，且预约属于当前用户

成功响应 `data`：

```json
{
  "id": 501,
  "appointmentId": 1001,
  "doctorId": 1,
  "doctorName": "张明华",
  "chiefComplaint": "咳嗽三天",
  "presentIllness": "无明显发热",
  "diagnosis": "上呼吸道感染",
  "treatmentPlan": "对症治疗，多饮水",
  "doctorAdvice": "如症状加重请复诊",
  "createTime": "2026-11-09 09:30:00",
  "updateTime": "2026-11-09 09:35:00"
}
```

尚未生成病历时返回 `404`，消息为“暂无病历”。

### 8.8 提交评价

`POST /api/appointments/{id}/review`

权限：`USER`，且预约属于当前用户

请求：

```json
{
  "rating": 5,
  "tags": ["态度好", "讲解清楚"],
  "comment": "医生讲解很耐心"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `rating` | integer | 是 | `1` 到 `5` |
| `tags` | string[] | 否 | 最多 5 个，每个最大 20 字符 |
| `comment` | string | 否 | 最大 500 字符 |

规则：

- 只有 `completed` 预约可评价。
- 每个预约只能评价一次，由 `appointment_review.uk_appointment` 保证。
- `tags` 在数据库中以英文逗号拼接后保存到 `appointment_review.tags`。
- 成功响应 `data` 为评价对象。

## 9. 处方接口

### 9.1 按预约查看处方

`GET /api/appointments/{id}/prescription`

权限：`USER`，且预约属于当前用户

成功响应 `data` 为处方对象。尚无处方时返回 `404`，消息为“暂无处方”。

### 9.2 按处方 ID 查看

`GET /api/prescriptions/{id}`

权限：`USER`，且处方属于当前用户

处方对象：

```json
{
  "id": 801,
  "appointmentId": 1001,
  "userId": 2,
  "doctorId": 1,
  "doctorName": "张明华",
  "departmentName": "内科",
  "totalAmount": 54.50,
  "status": "unpaid",
  "payMethod": null,
  "payMethodText": null,
  "payTime": null,
  "createTime": "2026-11-09 09:35:00",
  "updateTime": "2026-11-09 09:35:00",
  "items": [
    {
      "id": 1,
      "medicineId": 1,
      "medicineName": "阿莫西林胶囊",
      "spec": "0.25g*24粒",
      "dosage": "1粒",
      "frequency": "每日3次",
      "days": 3,
      "quantity": 1,
      "unitPrice": 18.50,
      "amount": 18.50
    }
  ]
}
```

处方明细中的药名、规格和单价是开方时快照，药品基础数据后续修改不得改变历史处方。

### 9.3 支付处方

`POST /api/prescriptions/{id}/pay`

权限：`USER`，且处方属于当前用户

请求：

```json
{
  "payMethod": "balance"
}
```

规则：

- 仅 `unpaid` 可支付。
- 使用条件更新锁定处方状态，防止重复扣款。
- `balance` 支付必须原子扣减余额并写入 `balance_log`。
- 支付成功后处方状态为 `paid`。
- 预约保持 `pending_rx_pay`，由前端展示为“待取药”。
- 支付后不得再次支付；重复请求可返回当前处方，但不得重复扣款。

成功响应 `data` 为支付后的处方对象。

## 10. 消息通知接口

### 10.1 通知列表

`GET /api/notifications`

权限：`USER`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `current` | integer | 否 | 默认 `1` |
| `size` | integer | 否 | 默认 `10`，最大 `100` |
| `read` | boolean | 否 | 按已读状态筛选 |

成功响应为分页对象，通知项：

```json
{
  "id": 1,
  "type": "appointment",
  "title": "预约成功",
  "content": "您已成功预约内科张明华医生",
  "bizType": "appointment",
  "bizId": 1001,
  "read": false,
  "createTime": "2026-11-09 09:10:00"
}
```

数据库 `read_flag` 转换为接口布尔字段 `read`。

### 10.2 未读数量

`GET /api/notifications/unread-count`

权限：`USER`

成功响应 `data` 为整数：

```json
{
  "code": 200,
  "data": 3,
  "message": "success"
}
```

### 10.3 标记单条已读

`PUT /api/notifications/{id}/read`

权限：`USER`，且通知属于当前用户

成功响应：

```json
{
  "code": 200,
  "data": null,
  "message": "success"
}
```

通知不存在或不属于当前用户时返回 `404`。

### 10.4 全部标记已读

`PUT /api/notifications/read-all`

权限：`USER`

只更新当前用户的未读通知，成功响应 `data` 为本次更新数量。

## 11. 钱包接口

### 11.1 余额充值

`POST /api/wallet/recharge`

权限：`USER`

请求：

```json
{
  "amount": 100.00,
  "payMethod": "wechat"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `amount` | number | 是 | `0.01` 到 `10000.00` |
| `payMethod` | string | 是 | 仅 `wechat`、`alipay` |

成功响应：

```json
{
  "code": 200,
  "data": {
    "balance": 200.00,
    "logId": 9001
  },
  "message": "success"
}
```

事务规则：

- 使用 `UPDATE sys_user SET balance = balance + ? WHERE id = ?` 原子增加余额。
- 写入 `balance_log`，`type = recharge`，金额为正数。
- 模拟支付接口不得重复回调导致重复充值。

### 11.2 余额流水

`GET /api/wallet/logs`

权限：`USER`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `current` | integer | 否 | 默认 `1` |
| `size` | integer | 否 | 默认 `20`，最大 `100` |

成功响应为分页对象，流水项：

```json
{
  "id": 9001,
  "userId": 2,
  "type": "recharge",
  "amount": 100.00,
  "balanceAfter": 200.00,
  "orderId": null,
  "payMethod": "wechat",
  "remark": "余额充值",
  "createTime": "2026-11-09 10:00:00"
}
```

结果按 `createTime` 降序排列。

## 12. 管理后台接口

所有 `/api/admin/**` 接口仅允许 `ADMIN`。

管理后台列表接口默认使用分页对象。

### 12.1 数据概览

`GET /api/admin/dashboard/stats`

成功响应：

```json
{
  "code": 200,
  "data": {
    "todayAppointments": 12,
    "todayRevenue": 240.00,
    "pendingAppointments": 8,
    "completedAppointments": 36,
    "doctorCount": 36,
    "departmentCount": 12,
    "userCount": 2,
    "last7Days": [
      {
        "date": "2026-11-03",
        "appointmentCount": 5,
        "revenue": 100.00
      }
    ]
  },
  "message": "success"
}
```

### 12.2 科室管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/departments` | 分页查询科室，可传 `keyword`、`status` |
| `GET` | `/api/admin/departments/{id}` | 科室详情 |
| `POST` | `/api/admin/departments` | 新增科室 |
| `PUT` | `/api/admin/departments/{id}` | 修改科室 |
| `PUT` | `/api/admin/departments/{id}/status` | 启用或停用 |

新增或修改请求：

```json
{
  "name": "内科",
  "description": "呼吸、消化、内分泌等",
  "location": "门诊楼3楼",
  "sortOrder": 1,
  "status": 1
}
```

约束：

- `name` 必填，最大 100 字符。
- `description` 最大 500 字符。
- `location` 最大 200 字符。
- 停用科室前应确认该科室没有未完成预约；已存在排班不物理删除。

### 12.3 医生管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/doctors` | 分页查询，可传 `departmentId`、`keyword`、`status` |
| `GET` | `/api/admin/doctors/{id}` | 医生详情 |
| `POST` | `/api/admin/doctors` | 新增医生 |
| `PUT` | `/api/admin/doctors/{id}` | 修改医生 |
| `PUT` | `/api/admin/doctors/{id}/status` | 启用或停用 |

新增或修改请求：

```json
{
  "departmentId": 1,
  "name": "张明华",
  "title": "主任医师",
  "gender": 1,
  "avatar": "/uploads/common/doctor_m_1.jpg",
  "intro": "擅长呼吸系统疾病诊治",
  "status": 1
}
```

约束：

- `departmentId` 必须对应启用科室。
- `gender` 只能为 `1` 或 `2`。
- 停用医生后，小程序不得继续展示该医生。
- 医生账号由管理员初始化，不通过公开接口注册。

### 12.4 排班管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/schedules` | 分页查询，可传 `doctorId`、`startDate`、`endDate`、`status` |
| `GET` | `/api/admin/schedules/{id}` | 排班详情 |
| `POST` | `/api/admin/schedules` | 新增排班 |
| `POST` | `/api/admin/schedules/batch` | 批量新增排班 |
| `PUT` | `/api/admin/schedules/{id}` | 修改排班 |
| `PUT` | `/api/admin/schedules/{id}/status` | 停诊或恢复 |

新增请求：

```json
{
  "doctorId": 1,
  "scheduleDate": "2026-11-10",
  "timeSlot": "09:00",
  "totalQuota": 5,
  "status": 1
}
```

批量新增请求：

```json
{
  "doctorIds": [1, 2, 3],
  "startDate": "2026-11-10",
  "endDate": "2026-11-16",
  "timeSlots": ["08:00", "09:00", "10:00"],
  "totalQuota": 5
}
```

约束：

- 唯一键为 `doctorId + scheduleDate + timeSlot`。
- `totalQuota` 不得小于当前 `bookedCount`。
- 已有预约的排班不得物理删除。
- 停诊不等于取消已有预约，管理员必须分别处理已有预约。

### 12.5 预约管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/appointments` | 分页查询，可传 `status`、`doctorId`、`departmentId`、`patientName`、`startDate`、`endDate` |
| `GET` | `/api/admin/appointments/{id}` | 预约详情 |
| `PUT` | `/api/admin/appointments/{id}/cancel` | 管理员取消预约 |

管理员取消规则：

- 可取消 `unpaid`、`pending`、`checked_in`。
- `in_consultation`、`pending_rx_pay`、`completed`、`cancelled` 默认不允许直接取消。
- 取消后释放号源，并按照患者取消规则处理退款和通知。

### 12.6 处方与发药

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/prescriptions` | 分页查询，可传 `status`、`userId`、`doctorId` |
| `GET` | `/api/admin/prescriptions/{id}` | 处方详情 |
| `PUT` | `/api/admin/prescriptions/{id}/dispense` | 确认发药 |
| `PUT` | `/api/admin/prescriptions/{id}/cancel` | 取消未支付处方 |

发药规则：

- 仅 `paid` 可发药。
- 发药成功将处方改为 `dispensed`。
- 同一事务内将关联预约改为 `completed`。
- 可扣减 `medicine.stock`，库存不足时拒绝发药并返回 `409`。
- 重复发药必须幂等，不得重复扣库存。

取消处方规则：

- 仅 `unpaid` 可取消。
- 取消后若预约只有该处方且尚未完成，管理员需明确结束或保留预约后续处理。

### 12.7 药品管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/medicines` | 分页查询，可传 `keyword`、`status` |
| `GET` | `/api/admin/medicines/{id}` | 药品详情 |
| `POST` | `/api/admin/medicines` | 新增药品 |
| `PUT` | `/api/admin/medicines/{id}` | 修改药品 |
| `PUT` | `/api/admin/medicines/{id}/status` | 启用或停用 |

新增或修改请求：

```json
{
  "name": "阿莫西林胶囊",
  "spec": "0.25g*24粒",
  "unit": "盒",
  "price": 18.50,
  "stock": 200,
  "usageDesc": "口服，遵医嘱",
  "status": 1
}
```

约束：

- `price` 不得为负。
- `stock` 不得为负。
- 已被处方引用的药品不得物理删除，只允许停用。

### 12.8 用户管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/users` | 分页查询，可传 `keyword`、`role`、`status` |
| `GET` | `/api/admin/users/{id}` | 用户详情，不返回密码和 openid |
| `PUT` | `/api/admin/users/{id}/status` | 启用或禁用用户 |

请求体：

```json
{
  "status": 1
}
```

管理员不得通过该接口读取或修改密码哈希。

### 12.9 菜单与日志

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/menus` | 返回当前管理员可访问菜单 |
| `GET` | `/api/admin/logs/login` | 分页查询登录日志，可传 `username`、`status`、日期范围 |
| `GET` | `/api/admin/logs/operation` | 分页查询操作日志，可传 `username`、`module`、`status`、日期范围 |

菜单项：

```json
{
  "id": 1,
  "parentId": 0,
  "name": "数据概览",
  "path": "/admin/dashboard",
  "icon": "HomeFilled",
  "sortOrder": 1,
  "roles": "ADMIN"
}
```

## 13. 医生工作台接口

所有 `/api/doctor/**` 接口仅允许 `DOCTOR`，且强制使用 JWT 对应的 `doctorId`。

### 13.1 当前医生信息

`GET /api/doctor/me`

成功响应为当前医生对象，包含科室和账号展示字段。

### 13.2 工作台统计

`GET /api/doctor/stats`

成功响应：

```json
{
  "code": 200,
  "data": {
    "todayAppointments": 8,
    "waitingCount": 3,
    "inConsultationCount": 1,
    "completedCount": 4
  },
  "message": "success"
}
```

### 13.3 我的排班

`GET /api/doctor/schedules`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `startDate` | string | 否 | 开始日期 |
| `endDate` | string | 否 | 结束日期 |

返回数组，只包含当前医生排班。

### 13.4 我的预约

`GET /api/doctor/appointments`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | integer | 否 | 默认 `1` |
| `size` | integer | 否 | 默认 `10` |
| `status` | string | 否 | `pending`、`checked_in`、`in_consultation`、`pending_rx_pay`、`completed`、`cancelled` |
| `scheduleDate` | string | 否 | 就诊日期 |

成功响应为分页预约对象，只能包含当前医生数据。

### 13.5 预约详情

`GET /api/doctor/appointments/{id}`

只能查看本人预约。响应包含预约、病历和处方摘要。

### 13.6 开始接诊

`POST /api/doctor/appointments/{id}/start`

请求体为空。

规则：

- 仅 `checked_in` 可开始接诊。
- 仅预约对应的 `doctorId` 可操作。
- 使用条件更新状态为 `in_consultation`。
- 重复请求不得重置病历或状态。

### 13.7 保存病历

`PUT /api/doctor/appointments/{id}/clinical`

仅 `in_consultation` 可编辑。

请求：

```json
{
  "chiefComplaint": "咳嗽三天",
  "presentIllness": "无明显发热，无呼吸困难",
  "diagnosis": "上呼吸道感染",
  "treatmentPlan": "对症治疗，多饮水",
  "doctorAdvice": "如症状加重请复诊"
}
```

字段约束：

| 字段 | 最大长度 | 必填 |
|---|---:|---:|
| `chiefComplaint` | 500 | 是 |
| `presentIllness` | 1000 | 否 |
| `diagnosis` | 500 | 是 |
| `treatmentPlan` | 1000 | 否 |
| `doctorAdvice` | 500 | 否 |

接口采用创建或更新语义，每个预约最多一条 `consultation_record`。

成功响应 `data` 为病历对象。

### 13.8 查看病历

`GET /api/doctor/appointments/{id}/clinical`

只能查看本人预约对应病历。未创建时返回 `404`。

### 13.9 完成接诊并开方

`POST /api/doctor/appointments/{id}/complete`

仅 `in_consultation` 可完成，必须已有病历。

无处方完成：

```json
{
  "prescriptionItems": []
}
```

有处方完成：

```json
{
  "prescriptionItems": [
    {
      "medicineId": 1,
      "dosage": "1粒",
      "frequency": "每日3次",
      "days": 3,
      "quantity": 1
    }
  ]
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---:|---|
| `prescriptionItems` | array | 是 | 可为空数组 |
| `prescriptionItems[].medicineId` | integer | 是 | 药品必须启用 |
| `prescriptionItems[].dosage` | string | 否 | 最大 50 字符 |
| `prescriptionItems[].frequency` | string | 否 | 最大 50 字符 |
| `prescriptionItems[].days` | integer | 否 | 大于 `0` |
| `prescriptionItems[].quantity` | integer | 是 | 大于 `0` |

服务端规则：

- 不允许客户端提交或信任 `unitPrice`、`amount`，必须由服务端按当前药品价格计算。
- 药品名称、规格、单价写入处方快照。
- 明细数组不能包含重复 `medicineId`，重复时合并或返回 `400`。
- 无处方时预约状态改为 `completed`。
- 有处方时创建 `prescription` 和明细，状态为 `unpaid`，预约状态改为 `pending_rx_pay`。
- 完成接诊、病历保存、处方创建必须在同一事务内完成。
- 可使用请求头 `Idempotency-Key` 防止重复开方；同一预约只允许一个处方，由 `uk_prescription_appt` 保证。

成功响应：

```json
{
  "code": 200,
  "data": {
    "appointmentId": 1001,
    "appointmentStatus": "pending_rx_pay",
    "prescriptionId": 801,
    "prescriptionAmount": 54.50
  },
  "message": "success"
}
```

无处方完成时 `prescriptionId` 为 `null`，`appointmentStatus` 为 `completed`。

### 13.10 查询可用药品

`GET /api/doctor/medicines`

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `keyword` | string | 否 | 药名或规格模糊查询 |

该接口返回启用且有库存的药品数组，用于医生开方选择。

## 14. 关键数据对象与数据库映射

### 14.1 用户

| API 字段 | 数据库字段 |
|---|---|
| `id` | `sys_user.id` |
| `username` | `sys_user.username` |
| `nickname` | `sys_user.nickname` |
| `email` | `sys_user.email` |
| `phone` | `sys_user.phone` |
| `avatar` | `sys_user.avatar` |
| `balance` | `sys_user.balance` |
| `doctorId` | `sys_user.doctor_id` |
| `status` | `sys_user.status` |
| `role` | `sys_user.role` |

### 14.2 科室

| API 字段 | 数据库字段 |
|---|---|
| `id` | `department.id` |
| `name` | `department.name` |
| `description` | `department.description` |
| `location` | `department.location` |
| `sortOrder` | `department.sort_order` |
| `status` | `department.status` |
| `createTime` | `department.create_time` |
| `updateTime` | `department.update_time` |

### 14.3 医生

| API 字段 | 数据库字段或来源 |
|---|---|
| `id` | `doctor.id` |
| `departmentId` | `doctor.department_id` |
| `departmentName` | `department.name` |
| `name` | `doctor.name` |
| `title` | `doctor.title` |
| `gender` | `doctor.gender` |
| `avatar` | `doctor.avatar` |
| `intro` | `doctor.intro` |
| `status` | `doctor.status` |
| `rating` | 评价聚合 |
| `reviewCount` | 评价数量聚合 |
| `consultCount` | 已完成预约数量聚合 |
| `hasAvailableSlot` | 排班号源聚合 |

### 14.4 排班

| API 字段 | 数据库字段或来源 |
|---|---|
| `id` | `schedule.id` |
| `doctorId` | `schedule.doctor_id` |
| `scheduleDate` | `schedule.schedule_date` |
| `timeSlot` | `schedule.time_slot` |
| `totalQuota` | `schedule.total_quota` |
| `bookedCount` | `schedule.booked_count` |
| `remainQuota` | `totalQuota - bookedCount` |
| `status` | `schedule.status` |

### 14.5 预约

| API 字段 | 数据库字段或来源 |
|---|---|
| `id` | `appointment.id` |
| `userId` | `appointment.user_id` |
| `scheduleId` | `appointment.schedule_id` |
| `doctorId` | `doctor.id` |
| `doctorName` | `doctor.name` |
| `doctorTitle` | `doctor.title` |
| `departmentId` | `doctor.department_id` |
| `departmentName` | `department.name` |
| `scheduleDate` | `schedule.schedule_date` |
| `timeSlot` | `schedule.time_slot` |
| `patientName` | `appointment.patient_name` |
| `patientPhone` | `appointment.patient_phone` |
| `idCard` | `appointment.id_card` |
| `amount` | `appointment.amount` |
| `payMethod` | `appointment.pay_method` |
| `payTime` | `appointment.pay_time` |
| `status` | `appointment.status` |
| `remark` | `appointment.remark` |
| `prescriptionId` | 关联处方 ID |
| `prescriptionStatus` | 关联处方状态 |
| `prescriptionAmount` | `prescription.total_amount` |
| `hasConsultation` | 是否存在 `consultation_record` |
| `evaluated` | 是否存在 `appointment_review` |
| `createTime` | `appointment.create_time` |
| `updateTime` | `appointment.update_time` |

### 14.6 处方

| API 字段 | 数据库字段或来源 |
|---|---|
| `id` | `prescription.id` |
| `appointmentId` | `prescription.appointment_id` |
| `userId` | `prescription.user_id` |
| `doctorId` | `prescription.doctor_id` |
| `doctorName` | `doctor.name` |
| `departmentName` | `department.name` |
| `totalAmount` | `prescription.total_amount` |
| `status` | `prescription.status` |
| `payMethod` | `prescription.pay_method` |
| `payTime` | `prescription.pay_time` |
| `createTime` | `prescription.create_time` |
| `updateTime` | `prescription.update_time` |
| `items` | `prescription_item` 列表 |

处方明细：

| API 字段 | 数据库字段 |
|---|---|
| `id` | `prescription_item.id` |
| `medicineId` | `prescription_item.medicine_id` |
| `medicineName` | `prescription_item.medicine_name` |
| `spec` | `prescription_item.spec` |
| `dosage` | `prescription_item.dosage` |
| `frequency` | `prescription_item.frequency` |
| `days` | `prescription_item.days` |
| `quantity` | `prescription_item.quantity` |
| `unitPrice` | `prescription_item.unit_price` |
| `amount` | `prescription_item.amount` |

## 15. 并发、幂等和事务要求

### 15.1 号源并发

不得采用“先查询剩余号源，再普通更新”的流程。必须使用带条件的原子更新，并以 MySQL 影响行数判断是否成功：

```sql
UPDATE schedule
SET booked_count = booked_count + 1
WHERE id = ?
  AND status = 1
  AND booked_count < total_quota;
```

### 15.2 支付和退款

支付、余额扣减、预约状态更新、余额流水必须在同一事务中完成。必须使用状态条件更新避免重复支付和重复退款。

### 15.3 处方

同一预约只能有一个处方。数据库唯一键 `uk_prescription_appt` 是最终约束，应用层重复校验只作为友好提示。

### 15.4 Redis 的边界

Redis 可用于：

- 缓存科室、医生和近期排班。
- 登录失败计数和临时风控。
- JWT 注销黑名单。
- 热点排班短时防重复请求。

Redis 不可作为号源是否售出的唯一依据。Redis 不可用时，登录、挂号和支付主流程仍需依靠 MySQL 正常工作；缓存故障应降级而非返回全站 `500`。

### 15.5 重复提交

前端按钮应在请求期间禁用，但后端仍必须防止重复业务：

| 业务 | 最终约束 |
|---|---|
| 重复创建同一预约 | 业务查询或 `Idempotency-Key` |
| 重复支付预约 | `status = 'unpaid'` 条件更新 |
| 重复取消 | 状态条件更新和号源只释放一次 |
| 重复支付处方 | `prescription.status = 'unpaid'` 条件更新 |
| 重复发药 | `prescription.status = 'paid'` 条件更新 |
| 重复评价 | `appointment_review.uk_appointment` |
| 重复开方 | `prescription.uk_prescription_appt` |

## 16. 业务通知

以下事件应写入 `user_notification`：

| 事件 | `type` 建议值 | `biz_type` | `biz_id` |
|---|---|---|---|
| 预约创建或支付成功 | `appointment_success` | `appointment` | 预约 ID |
| 预约取消 | `appointment_cancelled` | `appointment` | 预约 ID |
| 医生开出新处方 | `prescription_created` | `prescription` | 处方 ID |
| 处方支付成功 | `prescription_paid` | `prescription` | 处方 ID |
| 处方已发药 | `prescription_dispensed` | `prescription` | 处方 ID |

通知写入失败不得回滚核心挂号、支付或处方事务。推荐核心事务提交成功后，通过事务事件或消息机制异步写入。

## 17. 安全与隐私

- 姓名、手机号、身份证号、病历、处方属于敏感信息，不得输出到普通日志。
- 身份证号如必须展示，应进行脱敏，例如只显示前 3 位和后 4 位。
- 所有管理接口必须经过角色校验。
- 医生接口必须同时校验角色和 `doctorId` 数据归属。
- 用户接口必须校验当前用户数据归属，不能仅依赖前端传参。
- 生产环境必须关闭 SQL 明文日志和详细异常堆栈。
- 数据库密码、JWT 密钥、微信 `AppID`、`AppSecret` 通过环境变量或本地未跟踪配置提供。
- 生产环境必须使用 HTTPS；局域网开发可使用 HTTP。
- CORS 只允许明确配置的前端地址，不使用任意来源与凭证组合。
- 当前默认允许 `http://localhost:5173`、`http://127.0.0.1:5173`、参考管理端 `http://192.168.0.48:5173`，以及本地开发常用的 `localhost/127.0.0.1` 端口 `8080`、`5500`；可用 `CORS_ALLOWED_ORIGINS` 以逗号分隔覆盖。
- 旧 `/admin/**` 接口仅为迁移兼容保留，现已纳入 JWT 拦截且只允许 `ADMIN`；新增管理接口必须使用本文件第 12 节的 `/api/admin/**` 契约。

## 18. 小程序兼容性验收

后端实现至少满足以下现有调用：

| 小程序方法 | 接口 |
|---|---|
| `getDepartments` | `GET /api/departments` |
| `getDoctors` | `GET /api/doctors` |
| `getDoctorById` | `GET /api/doctors/{id}` |
| `getSchedules` | `GET /api/schedules` |
| `getAppointments` | `GET /api/appointments` |
| `getAppointmentById` | `GET /api/appointments/{id}` |
| `createAppointment` | `POST /api/appointments` |
| `payAppointment` | `POST /api/appointments/{id}/pay` |
| `cancelAppointment` | `PUT /api/appointments/{id}/cancel` |
| `checkInAppointment` | `PUT /api/appointments/{id}/check-in` |
| `getConsultation` | `GET /api/appointments/{id}/consultation` |
| `getPrescriptionByAppointment` | `GET /api/appointments/{id}/prescription` |
| `getPrescription` | `GET /api/prescriptions/{id}` |
| `payPrescription` | `POST /api/prescriptions/{id}/pay` |
| `submitAppointmentReview` | `POST /api/appointments/{id}/review` |
| `triageAnalyze` | `POST /api/triage/analyze` |
| `getNotifications` | `GET /api/notifications` |
| `getUnreadNotificationCount` | `GET /api/notifications/unread-count` |
| `markNotificationRead` | `PUT /api/notifications/{id}/read` |
| `markAllNotificationsRead` | `PUT /api/notifications/read-all` |
| `passwordLogin` | `POST /api/auth/login` |
| `wxLogin` | `POST /api/auth/wx-login` |
| `fetchMe` | `GET /api/auth/me` |
| `updateProfile` | `PUT /api/profile` |
| `uploadAvatarFile` | `POST /api/files/upload` |
| `bindAvatar` | `PUT /api/profile/avatar` |
| `walletApi.recharge` | `POST /api/wallet/recharge` |
| `walletApi.fetchBalanceLogs` | `GET /api/wallet/logs` |

## 19. 联调验收用例

### 19.1 患者主流程

1. 公开查询科室、医生和未来排班。
2. 登录患者账号并获得 JWT。
3. 选择有剩余号源的排班创建预约。
4. 使用余额或模拟支付完成挂号。
5. 预约状态从 `unpaid` 变为 `pending`。
6. 到院签到后变为 `checked_in`。
7. 医生开始接诊后变为 `in_consultation`。
8. 医生保存病历并开方后变为 `pending_rx_pay`。
9. 患者支付处方后处方变为 `paid`，小程序展示“待取药”。
10. 管理员发药后处方变为 `dispensed`，预约变为 `completed`。
11. 患者查看病历和处方并提交一次评价。

### 19.2 并发与异常

1. 两个用户同时预约最后一个号源，只允许一个成功。
2. 同一预约先后发送两次支付请求，只扣款一次。
3. 同一预约先后发送两次取消请求，只退号一次。
4. 用户访问他人预约详情返回 `403` 或 `404`。
5. 医生访问其他医生预约返回 `403`。
6. 处方未支付时发药返回 `400`。
7. 同一预约重复评价返回 `409`。
8. Redis 停止时，MySQL 主流程仍可完成。
9. JWT 过期后返回 `401`，小程序清理本地登录态。

## 20. 后端实现顺序

建议按以下顺序实现，保证三个客户端尽快开始联调：

1. 统一响应、异常处理、CORS、JWT、角色权限和当前用户。
2. 登录、当前用户、科室、医生、排班公共查询。
3. 预约创建、号源事务、支付、取消和详情。
4. 病历、处方、通知和钱包。
5. 管理后台 CRUD、数据概览和处方发药。
6. 医生工作台和智能分诊降级。
7. 并发用例、权限用例和端到端联调。

## 21. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| `1.0` | `2026-11-09` | 基于现有 SQL 和小程序调用完成首版全量接口契约 |
