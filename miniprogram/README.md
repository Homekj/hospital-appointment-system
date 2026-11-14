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

开发者工具本地联调可使用本机可达的后端地址，并在「详情 → 本地设置」勾选不校验合法域名。当前 `baseUrl` 是局域网 IP，手机须连接电脑所在的同一网络；蜂窝网络无法访问。手机普通预览不会继承本地不校验选项；稳定预览和体验版应使用可访问的 HTTPS 域名，在微信公众平台「开发管理 → 开发设置 → 服务器域名」配置 `request 合法域名`，头像上传还需要配置 `uploadFile 合法域名`。开发阶段可在同一局域网使用「真机调试」验证。

## 设计系统

- 主色：`#00B894`（见 `styles/tokens.wxss`）
- 组件：`components/` 目录
- Mock：`mock/` 目录

## 对接后端

将页面中的 `require('../../mock/...')` 替换为 `require('../../utils/api')` 对应方法。
# 本地固定金额支付演示

当前小程序的微信支付入口仍是项目内的模拟流程：等待后调用本项目后端更新订单，不会请求微信支付网关，也不会产生真实扣款。若要用一分钱演示挂号单流程，启动后端前设置环境变量 `APPOINTMENT_REGISTRATION_FEE=0.01`，再重启后端并新建预约；已有预约金额不会改变。未设置时挂号费保持 `20.00`。

真实微信支付仍需商户号、与商户号绑定的小程序 AppID、商户 API 私钥/证书序列号、API v3 密钥及支付回调配置。不要复用案例中的商户凭据。
