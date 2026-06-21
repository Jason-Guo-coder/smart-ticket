# SmartTicket 智工单 · 架构与开发约定（ARCHITECTURE.md）

> 版本：v1.1｜更新日期：2026-06-21
>
> 本文件是后端技术实现与工程约束的唯一事实来源。业务规则见 [`PRD.md`](./PRD.md)，视觉规范见 [`DESIGN.md`](./DESIGN.md)。
> **任何代码改动都必须遵守本文件 §6 开发约束与 §7 禁止破坏的逻辑。**

---

## 1. 技术栈（已限定，不得替换）

| 层 | 选型 | 说明 |
|---|---|---|
| 框架 | **Spring Boot**（3.x，JDK 21） | 单体应用 |
| ORM | **MyBatis-Plus** | 分页、逻辑删除、乐观锁、自动填充 |
| 数据库 | **MySQL** 8.x | 主存储 |
| 缓存 | **Redis** | 多场景（见 §5） |
| 认证 | **JWT** | 无状态登录 |
| 授权 | **Spring Security** | RBAC 动态权限 |
| AI | **大模型 HTTP API** | 结构化 JSON 输出 |
| 前端 | **Vue3 + Element Plus** | 演示用 |

### 1.1 硬性技术约束（红线）
- ❌ **不使用 RabbitMQ / 任何 MQ**——异步通知用站内状态轮询。
- ❌ **不使用 Spring Cloud / 微服务**——单体应用，禁止拆分。
- ❌ **不引入对象存储**（MVP）——附件本地存储或仅存 URL。
- ✅ AI 只占 `ai` **一个模块**，不得在其它模块散布大模型调用。

---

## 2. 模块划分（按业务领域）

```
auth        登录 / JWT 签发与校验 / 登出（token 黑名单）
user        用户、角色、权限（RBAC）
ticket      工单核心：提交 / 查询 / 状态流转 / 时间线
dispatch    派单：手动 / 自动 / 采纳 AI 建议
ai          AI 辅助分类与相似检索（含超时、重试、缓存、降级）
evaluation  评价
stats       看板与绩效统计
sla         SLA 超时扫描（定时任务）
common      统一返回 / 全局异常 / AOP 审计日志 / 限流 / 幂等 / 工具
```

模块与 PRD 功能（F1–F12）一一对应，详见 PRD §4。

---

## 3. 目录结构（按模块分包 + 模块内三层）

顶层**按业务模块分包**（与 §2 / PRD 的 9 个模块对齐，高内聚）；每个模块内部仍保留 **controller（接口层）→ service（业务层）→ mapper（数据访问层）** 三层职责；`common / config` 为全局横切，被各模块复用。

```
smart-ticket/
├── doc/                              # PRD.md / DESIGN.md / ARCHITECTURE.md
├── src/main/java/com/smartticket/
│   ├── SmartTicketApplication.java
│   │
│   ├── auth/                         # 模块：登录 / JWT / 登出黑名单
│   │   ├── controller/AuthController.java
│   │   ├── service/{ AuthService, impl/AuthServiceImpl }
│   │   └── dto/  vo/                 # 复用 user 模块的实体与 mapper
│   │
│   ├── user/                         # 模块：用户 / 角色 / 权限（RBAC）
│   │   ├── controller/{ UserController, RoleController }
│   │   ├── service/{ UserService, RoleService, impl/ }
│   │   ├── mapper/{ SysUserMapper, SysRoleMapper, SysPermissionMapper }
│   │   ├── entity/{ SysUser, SysRole, SysPermission, SysRolePermission }
│   │   └── dto/  vo/
│   │
│   ├── ticket/                       # 模块：工单核心（提交/查询/状态机/时间线）
│   │   ├── controller/TicketController.java
│   │   ├── service/{ TicketService, impl/TicketServiceImpl }   # 状态机集中于此
│   │   ├── mapper/{ TicketMapper, TicketLogMapper, TicketSolutionMapper }
│   │   ├── entity/{ Ticket, TicketLog, TicketSolution }
│   │   ├── enums/{ TicketStatus, Priority, Category }
│   │   └── dto/  vo/
│   │
│   ├── dispatch/                     # 模块：派单（手动/自动/采纳 AI）
│   │   ├── controller/DispatchController.java
│   │   ├── service/{ DispatchService, impl/ }                  # 乐观锁 + 分布式锁
│   │   └── dto/  vo/
│   │
│   ├── ai/                           # 模块：AI 辅助（独立内聚，对外仅一个入口）
│   │   ├── AiAssistService.java          # 对外唯一入口
│   │   ├── client/AiHttpClient.java      # HTTP 调用 + 超时
│   │   ├── prompt/PromptBuilder.java     # Prompt 拼装
│   │   ├── parser/AiResultParser.java    # JSON 校验 / 重试
│   │   ├── cache/AiResultCache.java      # Redis 结果缓存
│   │   └── model/AiSuggestion.java       # {category, priority, suggestedEngineer, similarTickets[]}
│   │
│   ├── evaluation/                   # 模块：评价
│   │   ├── controller/EvaluationController.java
│   │   ├── service/{ EvaluationService, impl/ }
│   │   ├── mapper/EvaluationMapper.java
│   │   ├── entity/Evaluation.java
│   │   └── dto/  vo/
│   │
│   ├── stats/                        # 模块：看板与绩效统计
│   │   ├── controller/StatsController.java
│   │   ├── service/{ StatsService, impl/ }                     # 绩效排行榜 ZSet
│   │   └── vo/
│   │
│   ├── sla/                          # 模块：SLA 超时
│   │   ├── job/SlaScanJob.java                                 # 定时扫描标红
│   │   └── service/{ SlaService, impl/ }
│   │
│   ├── common/                       # 全局横切关注点（被各模块复用）
│   │   ├── result/Result.java            # 统一返回体
│   │   ├── exception/                    # 全局异常 @RestControllerAdvice + BizException
│   │   ├── aspect/AuditLogAspect.java    # AOP 审计日志「写入」
│   │   ├── aspect/IdempotentAspect.java  # 幂等
│   │   ├── ratelimit/RateLimiter.java    # Redis 滑动窗口限流
│   │   ├── security/                     # JWT 过滤器 / RBAC 鉴权
│   │   ├── audit/                        # 审计日志「查询」：controller + service + mapper + entity
│   │   └── util/
│   │
│   └── config/                       # SecurityConfig / RedisConfig / MyBatisPlusConfig / JwtConfig ...
│
├── src/main/resources/
│   ├── application.yml
│   ├── mapper/                       # MyBatis XML（按模块建子目录：ticket/、user/ ...）
│   └── db/schema.sql                 # 建表脚本
└── pom.xml
```

> **结构要点**：
> 1. 顶层即业务模块——改一个功能（如"派单"）只在 `dispatch/` 一个包内操作，高内聚、好定位。
> 2. 模块内部仍是三层职责（见 §5.1），**不串层**。
> 3. `ai` 模块自包含，对外仅暴露 `AiAssistService` 一个入口（红线，见 §6.4）。
> 4. 审计日志：**写入**由 `common` 的 AOP 统一完成，**查询**（管理员审计页）放 `common/audit/`，避免散落。
> 5. 前端 `vue-frontend/` 独立目录，按 Element Plus 常规结构组织，落地 DESIGN.md 规范，不在本文件展开。

---

## 4. 核心数据表（概要）

| 表 | 关键字段 |
|---|---|
| `sys_user` | id, username, password, role_id, dept, status |
| `sys_role` | id, role_name, code |
| `sys_permission` | id, perm_code, uri |
| `sys_role_permission` | role_id, permission_id |
| `ticket` | id, ticket_no, title, content, category, priority, status, creator_id, assignee_id, **version（乐观锁）**, sla_deadline, create_time, update_time, deleted |
| `ticket_log` | id, ticket_id, action, operator_id, remark, create_time（**时间线 + 工单内审计双用途**） |
| `ticket_solution` | id, ticket_id, engineer_id, solution_text, create_time |
| `evaluation` | id, ticket_id, score, tags, comment, create_time |
| `engineer_profile` | user_id, category_skills, current_load |
| `sys_audit_log` | id, operator_id, role, action_type, target, ip, result, create_time（**全局操作审计**：登录/派单/权限变更等非工单操作） |

> 说明：`ticket_log` 记录工单维度的流转时间线；`sys_audit_log` 记录跨业务的全局操作（登录、用户/权限变更等），由 `common` 的 AOP 写入。
> 通用字段约定：`create_time / update_time` 由 MyBatis-Plus 自动填充；逻辑删除用 `deleted` 字段。

---

## 5. 服务层约定

### 5.1 分层职责（严格）
- **Controller**：只做「接收参数 → 鉴权（注解）→ 调 service → 包装 Result」。**禁止**写业务逻辑、禁止直接调 mapper。
- **Service**：业务规则、状态机校验、事务边界、跨表编排、调用 ai/cache。接口与实现分离（`XxxService` + `XxxServiceImpl`）。
- **Mapper**：只做数据访问，继承 `BaseMapper`；复杂查询写 XML。**禁止**在 mapper 里写业务判断。
- **跨模块调用**：只允许 controller→service、service→其它模块的 service（接口）/ 本模块 mapper；**禁止**跨模块直接调别人的 mapper。

### 5.2 通用约定
- **统一返回体**：所有接口返回 `Result<T>`（code / message / data），不裸返实体。
- **入参 / 出参分离**：入参用 `dto`，出参用 `vo`，**禁止**直接把 `entity` 暴露给前端。
- **事务**：写操作用 `@Transactional`；凡「业务变更 + 写 ticket_log」必须在同一事务内，保证一致性。
- **异常**：业务异常抛自定义 `BizException`，由全局 `@RestControllerAdvice` 统一转 `Result`；不在 controller 里 try-catch 吞异常。
- **枚举驱动**：状态 / 优先级 / 类别 / 角色用枚举，禁止魔法字符串散落。
- **状态流转集中**：工单状态变更统一走 `TicketService` 的状态机方法，禁止在多处直接 `set status`。

### 5.3 Redis 使用场景

| 场景 | 用法 |
|---|---|
| 热点工单 / 字典缓存 | 普通 KV / Hash |
| 防刷提交 | **滑动窗口限流** |
| 工单号生成 | **原子自增**（INCR）|
| 工程师绩效排行榜 | **ZSet** |
| 派单防并发重复指派 | **分布式锁** |
| AI 结果缓存 | 相同描述短期复用，控成本 |
| token 黑名单 | 登出 / 强制下线 |

### 5.4 安全与鉴权
- JWT 无状态登录；登出将 token 写入 Redis 黑名单。
- Spring Security 实现 **RBAC**：角色—权限—接口（uri）映射，**每个核心接口都要鉴权**。
- 密码加密存储（BCrypt），严禁明文。

---

## 6. AI 引用机制（重点）

AI 是点睛模块，全系统**仅 `ai` 模块**对接大模型，其它模块通过 `AiAssistService` 单一入口调用。

### 6.1 调用时序
```
工单提交（TicketService）
  → 落库（状态=待派单，生成工单号）
  → 调用 AiAssistService.analyze(title, content)：
      1. 先查 Redis 缓存（key = hash(title+content)）→ 命中直接返回
      2. PromptBuilder 拼 Prompt（强约束输出 JSON）
      3. AiHttpClient 调大模型 API（超时 3s）
      4. AiResultParser 解析 + 校验 JSON；失败重试 1 次
      5. 成功 → 写入 Redis 缓存 → 返回 AiSuggestion
  → 任一环节失败 → 返回"无 AI 建议"（降级），工单照常进入待派单
```

### 6.2 契约
- **入参**：`title`、`content`。
- **出参** `AiSuggestion`：`{ category, priority, suggestedEngineer, similarTickets[] }`。
- **超时**：3s（可配置 `application.yml`）。
- **重试**：解析失败重试 1 次。
- **降级**：任何异常（超时 / 限流 / 非法 JSON）→ 返回空建议，**不抛异常打断主流程**。
- **缓存**：相同描述短期复用，降低成本与延迟。

### 6.3 相似工单（MVP 实现）
- 用「类别 + 关键词 LIKE」查 `ticket` + `ticket_solution`，取 Top N。
- 向量检索（embedding + 余弦相似度）为**进阶项，不进 MVP**。

### 6.4 红线
- ❌ 主流程**强依赖** AI——AI 失败必须能人工兜底。
- ❌ 在 ticket / dispatch 等模块里直接拼 Prompt 或调 HTTP——必须经 `AiAssistService`。

---

## 7. 开发约束（必须遵守）

1. **守住范围**：严格执行 PRD §5 功能黑名单，不擅自加 MQ / 微服务 / 对象存储 / 向量检索 / 推送。
2. **模块化 + 三层不串层**：顶层按模块分包；模块内 controller 不碰 mapper，mapper 不写业务，service 不返回 entity；不跨模块直调别人的 mapper。
3. **统一基建先行**：Result、全局异常、审计 AOP、限流、幂等、安全属于 `common`，新功能复用而非重写。
4. **事务一致性**：派单 / 状态变更 / 评价等写操作，业务 + 写日志同事务。
5. **并发安全**：派单走「乐观锁（version）+ Redis 分布式锁」。
6. **幂等**：提交工单、评价等写接口做幂等（AOP + 唯一键 / token）。
7. **审计全覆盖**：登录、派单、状态流转、评价、用户 / 权限变更必须写审计日志（AOP 统一）。
8. **配置外置**：超时、限流阈值、SLA 时长、AI key 等放 `application.yml`，不硬编码。
9. **枚举与常量**：状态 / 角色 / 类别用枚举，URI / 权限码集中管理。
10. **最小改动**：改 Bug / 加功能只动必要文件，不顺手重构无关代码、不改公共契约。

---

## 8. 禁止破坏的逻辑（改动时严禁违反）

> 以下是系统的"承重墙"，任何改动都不得破坏。Review 时逐条核对。

| # | 不可破坏的逻辑 |
|---|---|
| B1 | **工单状态机**：只允许 `待派单→处理中→待验收→已完成→已评价` 及合法回退（转派→待派单、验收驳回→处理中）；非法流转必须被服务层拒绝。 |
| B2 | **状态变更必写日志**：每次流转写 `ticket_log`，与业务变更同事务。 |
| B3 | **派单并发安全**：乐观锁 version + 分布式锁，同一待派单工单并发只成功一次。 |
| B4 | **RBAC 鉴权**：核心接口的权限校验不可绕过 / 删除。 |
| B5 | **AI 降级兜底**：AI 失败不得阻断工单进入待派单；不得让主流程强依赖 AI。 |
| B6 | **统一返回 / 异常**：不得绕过 Result 与全局异常处理裸返数据或吞异常。 |
| B7 | **审计日志**：关键操作日志写入不可省略。 |
| B8 | **SLA 扫描**：定时任务必须能标记超时工单，看板据此预警。 |
| B9 | **幂等与限流**：提交 / 评价的幂等、提交限流不可移除。 |
| B10 | **技术红线**：不得引入 MQ / Spring Cloud / 对象存储（违反即破坏架构定位）。 |
| B11 | **模块边界**：不得跨模块直调他模块 mapper，不得在 `ai` 外调用大模型。 |

---

## 9. 验收标准（技术侧）

### 9.1 与 PRD 主线一致
端到端跑通黄金路径（PRD §7.1），且过程中：每个核心接口有鉴权、有审计日志、AI 故障可人工兜底。

### 9.2 技术验收清单

| 维度 | 验收点 |
|---|---|
| 分层 | 模块内 controller / service / mapper 职责清晰，无串层、无跨模块直调 mapper |
| 鉴权 | 核心接口 RBAC 校验生效，越权返回 403 |
| 状态机 | 非法流转被拒；流转必写 ticket_log |
| 并发 | 并发派单只成功一次（乐观锁 + 锁验证用例通过） |
| 事务 | 派单 / 状态变更 / 评价的「业务 + 日志」原子提交，异常回滚 |
| AI | 超时 / 非法 JSON 触发重试与降级；缓存命中可复用 |
| Redis | 限流、工单号自增、排行榜 ZSet、分布式锁、token 黑名单均可验证 |
| 审计 | 关键操作有日志，可按操作类型 / 时间 / 操作人查询 |
| SLA | 定时任务扫描标记超时，看板高亮 |
| 幂等 | 重复提交 / 重复评价被拦截 |
| 统一规范 | 全部接口返回 Result；异常统一处理；入参 dto / 出参 vo |

### 9.3 建议实现顺序
`auth / user / ticket`（主链路）→ `ai`（点睛）→ `dispatch / evaluation`→ `stats / sla / 审计完善`。

---

> 本文件与 PRD.md、DESIGN.md 共同构成项目的工程契约。新增能力先更新对应文档，再写代码。
