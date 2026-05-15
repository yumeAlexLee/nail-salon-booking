# 修正履历 (Changelog)

## 2026-05-15 — v1.1.0 子选项系统 + UI优化

### 新增功能

#### 子选项（附加服务）系统
- **后端**：新增 `MenuItemOption` 实体、`MenuItemOptionMapper`、子选项 CRUD API
  - `GET /api/menu/{menuItemId}/options` — 获取某菜单的启用子选项（客人端）
  - `GET /api/menu/options/batch` — 批量查询子选项详情
  - `GET/POST/PUT/DELETE /api/menu/admin/options/*` — 后台子选项管理
- **管理后台**：菜单管理 → 每个项目新增「选项管理」按钮，支持添加/编辑/删除子选项
- **客人端选择**：点击菜单项 → 弹出子选项面板（多选），选后进入选时间
- **动态时长**：DateTimeSelect 根据主服务+子选项总时长计算可用时间段
- **子选项初始数据**（61条）：贴钻小/中/大、珍珠、金箔、贝壳片、闪粉渐变、立体花朵、每指彩绘
  - 覆盖：本甲(单色/裸色/猫眼/法式)、美甲(单色/猫眼/法式/渐变)、款式甲(叠加层次/人物手绘/手工造型)

#### 卸甲功能整合
- 菜单中移除「卸甲」标签页（移至填写信息页）
- BookingForm 卸甲选项增加价格显示
- 卸甲费用计入服务总价和定金计算

### Bug修复

| 问题 | 原因 | 修复 |
|------|------|------|
| `notifyDepositRefunded` 找不到符号 | NotifyService 缺少该方法 | 补充退款通知方法 |
| `ALTER TABLE ... IF NOT EXISTS` 启动失败 | MySQL 8.0 不支持该语法 | 手动加列，移出 schema.sql |
| 子选项显示「暂无附加选项」 | 旧 jar 不含选项 API 端点 | 重新打包部署 |

### 技术调整

- 子选项弹窗改用 teleport + 自定义 overlay（限制在移动容器内，不超出左侧视觉区）
- 更新 `data-options.sql` seed 文件同步最新数据
- `ReservationRequest` 增加 `menuItemId`、`optionIds`、`totalDuration` 字段
- `Reservation` 实体增加 `menuItemId`、`selectedOptions` 字段
