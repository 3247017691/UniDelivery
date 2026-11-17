# UniDelivery 运行手册（Runbook）

## 1. 上线前清单（阻塞项，逐项留痕）

| # | 事项 | 操作 | 验证 |
|---|---|---|---|
| P1 | adv 白名单（邀请制） | 平台运营确认并留存记录 | `POST /api/admin/whitelist {advertiserId,name,evidence}`；预检 WHITELIST 项 pass |
| P2 | 抖音号全域投放授权 | 投放平台操作 | `GET /api/precheck` AWEME_AUTH pass（auth_status=AUTHRIZED 且 auth_scenarios 含 VIDEO） |
| P3 | 商品录入升级版短剧漫剧库 | 平台新建库+商品（或 MAPI create） | `POST /api/products/record`（orgId/platformId/专辑ID）→ `POST /api/products/sync` ok=true |
| P4 | OAuth 应用与授权 | `GET /api/oauth/authorize-url` → 客户授权 → `POST /api/oauth/exchange` | oauth_token 落库；续期 job 日志无 ERROR |
| P5 | 品牌摸底 | 自动（预检 BRAND 项调品牌列表） | 有品牌时 brand_info 自动装配；读不到即 fail |
| P6 | SPI 订阅 | 开发者后台-订阅服务创建任务：回调 `https://<host>/spi/callback`，secret_key 配置到 `unidelivery.spi-secret`，`spi-verifier=hmacSha256SpiVerifier`；任务 ID 入 `spi.subscribe-task-ids` | challenge 验证 200；真实推送验签通过且消费 |
| P7 | RDS 订阅 | 连山控制台创建 MySQL（**务必北京机房**）→ 开放平台订阅 RDS → EIP/DTS 取数 | `unidelivery.rds.enabled=true` + url 含 `lianshan_rds_{taskId}`；ingest 日志有行数 |
| P8 | 报表口径 | `GET /api/reports/config?topics=UNI_PROJECT_DATA,UNI_PROJECT_MATERIAL_DATA` 核定维度/指标 → 写入 `unidelivery.reports[]` | `/api/reports/pull` 有 rows；日终对账 breaches=0 |

**G1 决策门**：P1–P3 任一未闭环 → 不进入实机创建；系统预检会 fail-closed 拦截，勿绕过。

## 2. 日常运行

- 定时任务：token 续期(5min)、审核轮询(1min)、上传轮询(30s)、T+1 报表(01:30)、日终对账(03:00)、授权巡检(08:00)、日志回拉(5min)、SPI 补拉+积压消费+RDS 取数(1min)。
- 审核不通过：告警 ERROR + `reject_reason_json` 落库；人工修正后 `POST /api/projects/{id}/update`。
- 素材对账：`POST /api/projects/{id}/materials/reconcile`（平台为真相源）。
- 审计查询：`mapi_audit`（逐次请求）、`op_journal`（业务写）、`platform_log`（平台事实）；`POST /api/oplogs/reconcile` 发现未追溯操作即告警。

## 3. 灰度与 go/no-go

1. 单测试账户、预算下限 300 元、素材 ≤200 起投。
2. 红线全绿才放量：预检全 pass；创建→审核→在投→追投→调控→报表有数→SPI/RDS 收到；素材对账差异 0；日终对账 ≤ 阈值（默认 1%）；无未追溯平台操作。
3. 任一红线未绿 → 不灰度，修复后对受影响里程碑重新验收。

## 4. 止损与回滚

- **暂停优先于删除**：`POST /api/delivery/status optStatus=DISABLE`（单批 ≤10）。
- 平台侧写操作不可事务回滚；止损 = 暂停 + 素材 DISABLE + 必要时 delete（保留 ≥1 素材）。
- 所有写有 journal 与平台日志双向痕迹；误操作追溯用 `op_journal.client_request_id` ↔ `platform_log`。
- 唯一标的误占：终态同步（sync）释放 active_flag；切勿手工改库绕过唯一索引。
- token 泄露/异常：作废授权重新走 P4；`invalidate` 由 40102 自动触发。

## 5. 告警

`unidelivery.alert-webhook-url` 配置后推送 {level,title,detail}；未配置时结构化日志必达（`ALERT[...]`）。
关键告警：拒审、授权失效、创建/追投/启停部分失败、异步上传失败、对账超阈值、未追溯平台操作、RDS 取数失败。

## 6. 已知限制与未核实项（如实）

- **单实例部署**：限流器进程内令牌桶；多实例需外置限流（Redis/网关）后再水平扩展。DB 唯一键已保证跨实例幂等/去重正确性。
- **素材报表无消耗指标**（手册评论区确认）：绩效口径按业务核定（项目级消耗分摊），看板须明示口径。
- **update 空值语义未核实**：不支持「传空清空」；field-mask 仅非 null 入参。
- **删除/投放日期/时段接口字段未逐页核实**：走 `/api/delivery/passthrough` 受审计透传，payload 按官方文档组装。
- **商品详情与列表状态枚举不一致**（0/1 vs STATUS_*）：以列表页 0/1 + audit 1/2/-1 为准。
- **monetization_mode 枚举差异**（正文 IAP/IAA_AND_IAP vs schema 含 IAA）：服务仅接受正文两值。
- **SPI 素材推送 content 字段**（project_ids vs material_ids）：两者都兼容解析。
- **RDS 指标列明细未抓**：通用平铺列映射（维度列分离），新列自动进 metrics_json。
- 完整未核实清单见 `docs/contracts-*.md` 文末。

## 7. 故障排查

| 症状 | 排查 |
|---|---|
| 502 MAPI_ERROR code=40100/40110/40130 | 限流：看 mapi_audit attempt/outcome；降 qps 配置或错峰 |
| 502 code=40102 | token 过期：自动 invalidate+续期；持续出现查 refresh_token 是否失效(40103/40107) |
| 502 code=40118 | 接口权限未开通：开发者后台申请 |
| 409 NOT_READY | path 未配置覆盖 / 账户未 OAuth / 商品未录入组织ID |
| 422 PRECHECK_FAILED | 看 failures 逐项原因与证据 |
| SPI 400 signature invalid | secret 与订阅任务 secret_key 不一致；或 spi-verifier 仍为 rejectAll |
| RDS ingest=-1 | 连山库连接/表名（周分表）/北京机房；看日志 table 名 |
