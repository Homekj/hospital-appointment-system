# 医院预约挂号 — 微信小程序

患者端 UI 已按设计图实现，当前使用 **mock 数据**，后端就绪后通过 `utils/api.js` 切换。

## 页面结构

| Tab/页面 | 路径 |
|----------|------|
| 首页 | `pages/index/index` |
| AI 导诊 | `pages/triage/index` → `pages/triage/result` |
| 订单 | `pages/order/list` → `detail` / `evaluate` |
| 我的 | `pages/mine/mine` |
| 排班查询 | `pages/schedule/list` → `detail` |
| 预约挂号 | `pages/appointment/confirm` |

## 快速开始

```bash
cd miniprogram
npm install
npm run build:npm
```

微信开发者工具打开本目录，配置 `appid` 与 `utils/config.js` 的 `baseUrl`。

## 设计系统

- 主色：`#00B894`（见 `styles/tokens.wxss`）
- 组件：`components/` 目录
- Mock：`mock/` 目录

## 对接后端

将页面中的 `require('../../mock/...')` 替换为 `require('../../utils/api')` 对应方法。
