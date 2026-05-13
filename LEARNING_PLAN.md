# 美甲沙龙预约系统：全栈开发复习笔记

> **目标**：通过本项目，快速找回 Java (Spring Boot) + Vue 3 的开发手感，掌握前后端分离的核心流程。

---

## 📅 学习路线图

### 第一阶段：宏观架构与环境 (The Big Picture)
- [ ] **前后端分离原理**：理解前端请求 -> 后端接口 -> 数据库的闭环。
- [ ] **项目目录结构**：理清 `nail-salon-backend` 和 `nail-salon-frontend` 的职责。
- [ ] **配置与启动**：学习 `application.yml` (后端配置) 和 `vite.config.js` (前端配置)。

### 第二阶段：后端深挖 (Java / Spring Boot / MyBatis-Plus)
- [ ] **核心注解复习**：`@RestController`, `@Service`, `@Mapper`, `@Data` 等。
- [ ] **实体类 (Entity)**：数据库表如何映射到 Java 类。
- [ ] **业务逻辑 (Service)**：依赖注入 (@Autowired/@RequiredArgsConstructor) 的原理。
- [ ] **数据库操作 (MyBatis-Plus)**：BaseMapper 的基本增删改查。

### 第三阶段：前端深挖 (Vue 3 / Vite / Vant)
- [ ] **Composition API**：理解 `<script setup>`、`ref`、`reactive`。
- [ ] **组件化开发**：Vant 4 UI 组件的使用。
- [ ] **路由管理 (Vue Router)**：页面跳转与传参。
- [ ] **国际化 (Vue I18n)**：项目中如何处理多语言。

### 第四阶段：前后端联调 (The Bridge)
- [ ] **网络请求 (Axios)**：封装 API 请求，处理 Promise 和 async/await。
- [ ] **接口规范 (RESTful)**：GET/POST/PUT/DELETE 的使用场景。
- [ ] **跨域 (CORS)**：前端 Vite 代理与后端配置。

### 第五阶段：实战演练 (Real-world Flow)
- [ ] **完整流程追踪**：从前端“提交预约”按钮到后端数据库入库的完整链路。
- [ ] **小功能尝试**：尝试修改一个接口字段，并在前端展示。

---

## 📝 知识点随手记 (由你来补充)

### 1. Java 后端笔记
*   **Lombok**: 使用 `@Data` 自动生成 Getter/Setter。
*   **Spring Boot**: 使用 `@RestController` 返回 JSON 数据。
*   ...

### 2. Vue 前端笔记
*   **响应式**: `ref()` 用来定义基本类型，`reactive()` 用来定义对象。
*   **UI 库**: 这个项目用了 **Vant 4**，适合移动端。
*   ...

### 3. 踩坑记录 / 特别注意
*   (记录你在复习过程中遇到的问题)

---

## 🛠 常用命令备忘
*   **后端启动**: `mvn spring-boot:run` 或在 IDE 中运行启动类。
*   **前端启动**: `npm install` (第一次) -> `npm run dev`。
*   **数据库**: 查看 `docker-compose.yml` 中的数据库配置。
