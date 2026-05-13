# 💅 美甲预约系统 (Nail Salon Booking)

一个面向在日华人的美甲店在线预约系统，支持**预约管理、时段锁定、定金支付、多语言（日/中/英）**。

## 📋 功能一览

### 用户端

| 功能 | 说明 |
|------|------|
| **身份识别** | 输入姓名+联系方式，自动识别新客/老客 |
| **菜单浏览** | 按分类查看服务项目、价格、时长、示意图 |
| **选择日期时段** | 日历（未来14天，含日本节假日）、时段网格选择 |
| **填写预约信息** | 姓名、联系方式、卸甲类型、备注、参考图上传 |
| **定金支付** | 微信/支付宝二维码扫码支付，支持"我已付款"确认流程 |
| **我的预约** | 查看预约历史，支持取消预约 |
| **作品集** | 美甲作品瀑布流展示 |
| **来店指引** | 店铺路线图（车站→店铺） |

### 管理端

| 功能 | 说明 |
|------|------|
| **密码验证** | 口令进入后台看板 |
| **未来7天概览** | 每日空位数一览，点击进入当日详情 |
| **单日排班** | 查看/管理当日预约，锁定/解锁时段 |
| **预约管理** | 确认定金、标记完成、取消预约 |
| **菜单管理** | 增删改服务项目（名称、价格、时长、示意图等） |
| **店铺设置** | 营业时间、定金额度等配置 |
| **Telegram通知** | 客户"我已付款"时自动推送到店主手机 |

## 🛠 技术栈

| 层 | 技术 |
|----|------|
| **前端** | Vue 3 + Vant 4 + Vue Router + Vue I18n |
| **后端** | Java 17 + Spring Boot 3.2.5 + MyBatis-Plus |
| **数据库** | MySQL 8.0 |
| **部署** | Docker Compose |
| **通知** | Telegram Bot API（独立机器人推送） |

## 🚀 快速启动

### 前置要求

- JDK 17+
- Node.js 18+
- MySQL 8.0
- Maven（或用 Maven Wrapper）

### 1. 启动数据库

```bash
docker-compose up -d mysql
```

或手动创建 MySQL 数据库：

```sql
CREATE DATABASE IF NOT EXISTS nail_salon DEFAULT CHARACTER SET utf8mb4;
```

### 2. 启动后端

```bash
cd nail-salon-backend
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd nail-salon-frontend
npm install
npm run dev
```

### 4. 访问

- 用户端：`http://localhost:5173`
- 管理后台：`http://localhost:5173/admin`（默认口令：`888888`）

## 📂 项目结构

```
nail-salon-booking/
├── nail-salon-backend/          # Java Spring Boot 后端
│   └── src/main/java/com/nailsalon/booking/
│       ├── config/              # 配置（CORS、MyBatis等）
│       ├── controller/          # REST API 控制器
│       ├── dto/                 # 数据传输对象
│       ├── entity/              # 数据库实体
│       ├── exception/           # 全局异常处理
│       ├── mapper/              # MyBatis Mapper
│       └── service/             # 业务逻辑层
├── nail-salon-frontend/         # Vue 3 前端
│   └── src/
│       ├── views/               # 页面组件
│       ├── router/              # 路由配置
│       ├── api.js               # API 调用封装
│       ├── i18n/                # 国际化配置
│       └── style.css            # 全局样式
├── docker-compose.yml           # 容器编排
└── schema.sql                   # 数据库建表脚本
```

## 📄 数据库表

| 表名 | 说明 |
|------|------|
| `reservation` | 预约记录（含定金状态、时间、客户信息） |
| `locked_slot` | 锁定时间段（店主手动锁定/节假日） |
| `menu_item` | 服务项目菜单（名称、价格、时长、图片） |
| `store_settings` | 店铺配置（营业时间、定金额度等） |

## 🔗 相关链接

- [GitHub 仓库](https://github.com/yumeAlexLee/nail-salon-booking)
- [开发者笔记](https://hermes-agent.nousresearch.com/) — 使用 Hermes Agent 辅助开发

## 📝 License

MIT
