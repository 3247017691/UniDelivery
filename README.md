# UniDelivery — 短剧全域投放（MAPI）自动化服务

对接巨量引擎 Marketing API「全域投放（短剧版）」的 Java 后端服务：
投前预检 → 项目创建 → 素材管理 → 投中调控 → 投后报表 → SPI/RDS 实时订阅 → 审计对账。

依据：飞书《MAPI-全域投放（短剧版）操作手册》（rev 1362）+ 官方接口文档实抓契约
（`docs/contracts-project.md`、`docs/contracts-data.md`，含未核实清单）。
**不承载流量、不替代投放客户端**；所有写操作经用户/运营显式确认与 journal 审计。

## 快速开始

```bash
# 构建 + 全量测试（要求 0 警告 0 失败）
mvn -B -ntp clean test

# 打包
mvn -B -ntp package -DskipTests

# 开箱运行（H2 文件库，无需外部依赖；定时任务关闭）
java -jar app/target/uni-delivery-app-1.0.0.jar --spring.profiles.active=local

# 生产运行（MySQL）
docker compose up -d
DB_URL=jdbc:mysql://localhost:3306/uni_delivery DB_USER=uni_delivery DB_PASSWORD=uni_delivery_dev \
MAPI_APP_ID=<app_id> MAPI_APP_SECRET=<secret> \
java -jar app/target/uni-delivery-app-1.0.0.jar
```

环境：JDK 21 + Maven 3.9+；Spring Boot 3.4；MySQL 8（生产）/ H2（local 与测试）。

## 模块

| 模块 | 职责 |
|---|---|
| `mapi-client` | 纯 Java SDK：OAuth token 提供接口、全局+账户级令牌桶限流、指数退避重试（40100/40110/40130/50000/40102）、封套校验（code==0 才成功）、逐次审计、path 配置注册表、multipart 上传 |
| `app` | 业务服务：预检闸门 / 项目 / 素材 / 调控 / 上传 / 商品同步 / 报表双通道 / SPI 接收与补拉 / RDS 取数 / 操作日志对账 / 评论 / REST / 定时任务 |

## 安全闸门（设计红线）

- **预检 fail-closed**：白名单 / 抖音号全域授权 / 创编可用 / 商品可投 / 品牌摸底 / 唯一标的，任一读不到或失败即禁止创建；白名单未确认不向平台发起任何调用。
- **唯一标的**：`aweme_id × asset_id(商品ID)` 仅一个活跃全域项目（DB 唯一索引 + 服务闸门）。
- **标准停投二次确认**：创建全域会停止同标的在投标准项目（手册硬规则）→ 必须 `confirmStandardStop=true`，否则 428。
- **200 分批**：create 首批 ≤200 视频，超出走 `oc_project/material/create/` 追投（单次总量 ≤200）。
- **幂等**：`client_request_id` 唯一 journal；创建超时重试耗尽后**先查后认领**，杜绝重复项目。
- **增量更新 field-mask**：仅非 null 字段入请求体；空补丁拒绝。
- **部分成功不算成功**：`error_list` / `errors[]` 非空 → journal=PARTIAL + 告警。
- **删光保护**：平台禁止删光项目素材，本地先闸门拦截。
- **SPI 验签**：`X-Open-Signature` = HMAC-SHA256(secret, 原始 body)（官方算法）；默认 reject-all，secret 未配置绝不裸收；message_id 去重；口径未配置事件保持待消费不丢。
- **写路径审计**：每次平台调用逐次落 `mapi_audit`（独立事务）；业务写落 `op_journal`；平台日志回拉 + 大日志展开双向对账。

## REST 面（摘要）

`/api/precheck`、`/api/projects`(POST/GET)、`/api/projects/{id}/update|sync|reject-reasons`、
`/api/projects/{id}/materials/append|reconcile|delete|status`、
`/api/delivery/status|roi|monetization|passthrough`、
`/api/products/record|sync`、`/api/uploads/file|url|poll|labels`、
`/api/reports/pull|config|reconcile`、`/api/oplogs/sync|reconcile`、
`/api/comments/*`、`/api/oauth/authorize-url|exchange`、`/api/admin/whitelist`、
`/spi/callback`(GET+POST)、`/actuator/health|metrics|prometheus`。

错误语义：422 预检失败（带 failures）/ 428 需确认 / 409 标的冲突或未就绪 / 502 平台错误（带 code、request_id）/ 400 参数 / 500 内部（带消息，不掩盖）。

## 配置要点

| 键 | 说明 |
|---|---|
| `mapi.base-url / paths.*` | 平台 host 与 path 覆盖（uni/oc 迁移防御） |
| `mapi.retryable-codes` | 默认 [40100,40110,40130,50000]（官方附录核实） |
| `unidelivery.app-id/app-secret` | OAuth 应用凭证 |
| `unidelivery.spi-secret / spi-verifier` | SPI 验签密钥与实现 bean（默认 rejectAllSpiVerifier） |
| `unidelivery.spi.subscribe-task-ids` | SPI 补拉任务 ID 列表 |
| `unidelivery.reports[]` | 报表口径（topic/dimensions/metrics/granularity），以 config/get 实机为准 |
| `unidelivery.rds.*` | 连山云 MySQL 取数（enabled/url/username/password/task-id/topic） |
| `unidelivery.jobs.*` | 定时任务开关与频率 |
| `unidelivery.alert-webhook-url` | 告警推送（可选；日志必达） |

## 测试证据

`mvn clean test`：**30 通过 / 0 失败 / 0 错误 / 0 跳过**（SDK 契约 10 + 应用 20），
编译 `-Xlint:all -Werror` 0 警告。覆盖：限流重试、token 失效续期、封套/部分成功、
200 分批、幂等重放、确认闸门、标的冲突、预检 fail-closed、增量更新、删光保护、
素材对账、SPI 验签/去重/challenge/待消费、RDS 周分表取数、大日志对账、brand_info 映射。

## 文档

- [docs/runbook.md](docs/runbook.md) — 上线前清单、订阅配置、灰度、止损、已知限制
- [docs/design-and-contracts.md](docs/design-and-contracts.md) — 手册规则→代码映射、契约坑位、未核实清单、测试矩阵
- [docs/contracts-project.md](docs/contracts-project.md) / [docs/contracts-data.md](docs/contracts-data.md) — 官方契约实抓记录

## 边界与待办（如实声明）

实机联调依赖外部前置（白名单邀请、OAuth 授权、商品库录入、SPI/RDS 订阅开通），
本交付以 WireMock 契约 + H2 端到端验证为准；实机验收清单见 runbook。
单实例部署假设（限流器进程内）；水平扩展需外置限流，见 runbook。
