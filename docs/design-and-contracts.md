# 设计映射与契约记录

## 1. 手册规则 → 实现映射

| 手册规则/能力 | 实现落点 | 验证测试 |
|---|---|---|
| 白名单邀请制 | `advertiser.whitelisted+evidence`；预检 WHITELIST fail-closed，未确认不发远程调用 | E2ECreateTest.precheckFailBlocksCreate |
| 抖音号全域授权（auth_scenarios 含 VIDEO） | PreCheckService.checkAwemeAuth（AUTHRIZED 原文拼写）+ 每日巡检 | E2ECreateTest.fullCreate… |
| 创编可用抖音号（playlet_id=专辑ID 必填） | checkAwemeAvailable 分页 has_more | 同上 |
| 升级版短剧漫剧库 + 可投 + 有效 playlet_id | ProductSyncService（EBP/account_id=组织ID；status==1 && audit==1） | E2ECreateTest.seedProduct |
| brand_info 条件必填（2026-08-04） | checkBrand 装配（merge_brand_id→cdp_brand_id 等映射）；create 自动携带 | PureLogicTest.brandInfoMappingFollowsContract |
| 唯一标的 aweme_id×asset_id | uni_project uk_target(aweme,asset,active_flag)；终态置 NULL 释放 | E2ECreateTest.confirmationGateThenTargetConflict |
| 建全域停同标的标准项目 | confirmStandardStop 显式确认（428 闸门）+ confirmed_conflict 留痕 | 同上 |
| create ≤200 视频 | ProjectService.chunk + 追投 oc_project/material/create | E2ECreateTest.fullCreate（200+50 断言请求体） |
| update 增量语义 | UpdatePatch field-mask；空补丁 400 | E2EOperationsTest.incrementalUpdate… |
| 列表不返回素材 | 素材真相仅 material/get（游标分页） | E2EOperationsTest.reconcileUsesPlatformAsTruth |
| 部分成功语义（error_list/errors[]） | journal=PARTIAL + 告警；BatchOutcome.fullSuccess | E2ECreateTest.partialCreate…、E2EOperationsTest.materialStatusPartial… |
| 删除不允许删光 | MaterialService.delete 本地闸门 | E2EOperationsTest.deleteAllMaterialsBlocked… |
| 素材启停单批 1~100 | setStatus 分批 | 同上 partial 测试 |
| 调控单批 ≤10（status/roigoal） | DeliveryService.batchCall | E2EOperationsTest.deliveryStatusBatch… |
| 变现模式混合必带 IAA 链接 | updateMonetization 校验 | E2EOperationsTest.monetizationRequiresIaaUrl… |
| 「仅IAA」标签 + 回读 | UploadService labels 入参 + readLabels/hasLabel（file/video/get） | 契约测试 multipart；runbook R7 |
| 异步上传 tos 链接 + 轮询 | uploadByUrl + pollTasks（PROCESS/SUCCESS/FAILED，≤100 task_ids） | SDK/服务层；e2e 待实机 |
| SPI 验签 HMAC-SHA256(secret, raw body) | HmacSha256SpiVerifier（小写 hex、常数时间比较）；默认 rejectAll | SpiWebhookTest.invalidSignature…/validPush… |
| SPI challenge / 200/4xx/5xx 语义 | SpiWebhookController GET+POST | SpiWebhookTest.challengeVerificationEchoes |
| SPI message_id 去重 + 数据就绪→拉取 | subscription_event 唯一键；consume→pullRealtimeForIds | SpiWebhookTest.validPushConsumesOnceAndDedups |
| SPI 补拉（2 天内、APP-Access-Token） | SpiBackfillService（ad host + 应用级 token） | 服务层；e2e 待实机 |
| RDS 连山 MySQL 周分表 | RdsIngestService（lianshan_realtime_{周一}；稳定小时窗口；平铺列→metrics_json） | RdsIngestTest |
| 报表 data_topic 单数 / config 复数 | ReportService.pull / config | SpiWebhookTest（实时拉取请求形状） |
| 日终对账 | reconcile 同窗口共有指标相对差 ≤ 阈值 + 告警 | 服务层；e2e 待实机双通道 |
| 大日志 second_log_id → detail | OpLogService.sync 展开 detail_json | E2EOperationsTest.opLogSync… |
| 本地意图↔平台事实对账 | OpLogService.reconcile（±10min 窗匹配 journal） | 同上 |
| 限流三码 + 50000 + 40102 | MapiClient 重试/退避/invalidate | MapiClientTest.rateLimit…/expiredToken… |
| path 迁移 uni→oc（2026-03-04） | MapiEndpoint 默认 oc + MapiPaths 配置覆盖 | MapiClientTest.pathOverrideFromConfig |

## 2. 契约坑位（官方原文照录，代码已按此实现）

1. `material_status_frist`（frist 拼写）——素材一级状态应答字段，各素材列表均如此。
2. `auth_status` 枚举 `AUTHRIZED/AUTHRIZING/INVALID`（原文拼写）；请求过滤单数 `auth_scenario`、应答复数 `auth_scenarios`。
3. 品牌列表账户参数为 `account_id`（非 advertiser_id）；分页字段直接在 data 下。
4. `report/custom/get` 用单数 `data_topic`；`config/get` 用复数 `data_topics`；两页枚举数不一致（16 vs 15）。
5. `monetization_mode` 正文仅 IAP/IAA_AND_IAP，schema 另含 IAA —— 服务按正文两值校验。
6. material/create 横版枚举正文 `CREATIVE_IMAGE_MODE_VIDE`（缺 O）vs schema `..._VIDEO` —— 默认竖版规避，横版以实机为准。
7. 商品可投状态：列表 0/1 + audit 1/2/-1；详情 STATUS_*/AUDIT_STATUS_* —— 以列表为准。
8. `profession.album_id`=剧目ID（小程序载体）、`profession.playlet_id`=专辑ID（端原生载体）；手册 alt 的 play_id 不存在。
9. 起始解锁集 create 页 `start_pay_playlet` / update 页 `start_pay_episode`（本服务不写商品，仅记录）。
10. log_search 正文提 `search_log_id` 但字段表仅 `second_log_id` —— 按字段表。
11. SPI 封套 data 专页 object / 总页 json string —— 两种都解析。
12. SPI 素材专页 content.project_ids / 总页 material_ids —— 两者都解析。
13. 个别接口 host 为 ad.oceanengine.com（aweme/get、spi_task/get）—— MapiEndpoint.host()。
14. update 应答 error_list 为「分块更新部分失败」语义；create error_list 为素材失败明细（无 video_id，故落 lastError + 对账核定）。

## 3. 未核实项与处置（不臆造）

| 未核实 | 处置 |
|---|---|
| update 传 null/空串/空数组语义 | field-mask 仅非 null；不支持传空清空 |
| 删除/投放日期/时段接口字段 | 受审计透传 `/api/delivery/passthrough` |
| 评论六接口字段 | 透传 + 写操作 journal |
| 商品库 account_type 枚举（列表页空） | 按详情页 EBP |
| yuntu_category_id 来源接口 | 缺省不传（条件必填「若有则生效」） |
| R3_brand/R3_material 白名单查询接口 | 品牌/标签失败如实报错，不假装具备 |
| SPI 重试默认值/超时秒数 | 消费幂等 + 补拉兜底，不依赖平台重试参数 |
| RDS 各报表指标列明细 | 通用平铺映射，新列自动纳入 |
| 素材来源附录枚举 | 不使用 source 过滤 |

## 4. 测试矩阵摘要

- SDK（10）：封套/typed、业务错误不重试、限流重试、40102 续期、非 JSON 传输错误、GET query 编码、path 覆盖、multipart、令牌桶 burst/平滑 ×2。
- 应用（20）：上下文+schema validate(1)、纯逻辑(3)、创建链路(4)、运营链路(7)、SPI(4)、RDS(1)。
- 端到端均以 WireMock 扮演 MAPI（契约形状）、H2 扮演业务库；**实机验收另按 runbook §1/§3 执行**。
