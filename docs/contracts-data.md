# UniDelivery 接口契约采集记录（contracts-data.md）

- 采集时间：2026-11-17
- 采集原则：只记录真实抓到的内容；抓不到的标「未核实」，不编造。
- 采集方法说明（重要）：
  - A 部分 11 个 `open.oceanengine.com/labels/*/docs/*` 页面用 WebFetch 直抓**全部返回 JS 渲染空壳**（可见文本仅标题「API接口 - 商业开放平台」，HTML 约 1.6KB，含 `window.__EDEN_SSR_RENDER_CSR__` 标记）。
  - 第二次尝试改走该文档站自身的正文 JSON 接口（从站点 JS bundle 中定位）：
    `GET https://open.oceanengine.com/skiff/api/doc/client/node/get/?doc_id=<文档ID>&identify_key=<label密钥>&language=CHINESE`
    （identify_key 来自 `GET https://open.oceanengine.com/skiff/api/doc/client/label/tree/get/`，label 7「巨量营销-全部接口」与 label 8 共用同一 key）。
    返回 `code=0` + `data.title / data.path / data.content(HTML)`，即页面渲染所用的同一份正文。**下文 A 各节内容均来自该正文，属真实抓取**；字段表由 HTML 表格转换，代码示例卡片从 `value=` 属性 URL 解码还原。
  - B 部分用 lark-cli：`wiki +node-get`（成功，obj_type=docx）→ `docs +fetch --doc-format markdown` 输出出现 GBK 乱码（转换器编码缺陷，有字节丢失不可逆）→ 改 `--doc-format xml` 输出为干净 UTF-8，全文 9065 字符已完整读取。
  - 原始抓取件存于 `C:\Temp\contracts-DltLPE\`（doc1.json…doc24.json 及对应 .txt 转写）。

---

## A. 巨量引擎开放平台接口

### A1. 获取自定义报表可用指标和维度（report/custom/config/get）

- 来源：https://open.oceanengine.com/labels/7/docs/1755261744248832 （doc_id=1755261744248832，正文标题「获取自定义报表可用指标和维度」）
- 请求地址：`https://api.oceanengine.com/open_api/v3.0/report/custom/config/get/`
- 请求方法：**GET**

Header：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| Access-Token | 必填 | string | 授权 access_token，通过【获取Access token】接口获取 |

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户ID |
| data_topics | 必填 | string[] | 数据主题查询列表，允许值见下 |

`data_topics` 枚举（原文 16 项）：

| 值 | 含义 |
|---|---|
| BASIC_DATA | 营销基础数据 |
| QUERY_DATA | 搜索词数据 |
| BIDWORD_DATA | 关键词数据 |
| MATERIAL_DATA | 素材数据 |
| PRODUCT_DATA | 产品数据 |
| ONE_KEY_BOOST_DATA | 一键起量（巨量营销升级版） |
| DMP_DATA | 人群包数据 |
| VIDEO_DUARATION_DATA | 视频分秒数据（原文拼写如此） |
| MATERIAL_BOOST_DATA | 素材一键起量 |
| STD_BASIC_DATA | 标准投放-基础报表 |
| STD_MATERIAL_DATA | 标准投放-素材报表 |
| STD_QUERY_DATA | 标准投放-搜索词数据 |
| STD_BIDWORD_DATA | 标准投放-关键词数据 |
| STD_DMP_DATA | 标准投放-人群包数据 |
| STD_PRODUCT_DATA | 标准投放-产品数据 |
| STD_DPA_VIDEO_DATA | 标准投放-DPA视频粒度数据 |

应答字段：

| 字段 | 类型 | 描述 |
|---|---|---|
| code | number | 返回码，详见【附录-返回码】（见 A12） |
| message | string | 返回信息 |
| data | json | json 返回值 |
| data.list | object[] | 数据主题配置列表 |
| data.list[].data_topic | string | 数据主体（枚举同上，原文应答侧另列 STD_DPA_VIDEO_DATA 等） |
| data.list[].dimensions | object[] | 维度列表 |
| data.list[].dimensions[].field | string | 维度字段 |
| data.list[].dimensions[].name | string | 维度名称 |
| data.list[].dimensions[].description | string | 维度描述 |
| data.list[].dimensions[].sort_able | bool | 是否支持排序 |
| data.list[].dimensions[].filter_able | bool | 是否支持筛选 |
| data.list[].dimensions[].filter_config | object | 筛选条件 |
| …filter_config.type | number | 筛选字段类型 |
| …filter_config.operator | number | 筛选方式，枚举见下 |
| …filter_config.value_limit | number | 筛选字段传入数量上限 |
| …filter_config.range_value | object[] | 筛选字段枚举列表（label:string 名称 / value:string 具体值） |
| data.list[].dimensions[].exclusion_dims | string[] | 与维度互斥的维度列表 |
| data.list[].dimensions[].exclusion_metrics | string[] | 与维度互斥的指标列表 |
| data.list[].metrics | object[] | 指标列表 |
| data.list[].metrics[].field | string | 字段 |
| data.list[].metrics[].name | string | 字段名称 |
| data.list[].metrics[].description | string | 字段描述 |
| data.list[].metrics[].exclusion_dims | string[] | 与指标互斥的维度 |
| request_id | string | 请求日志id |

`operator` 枚举（filter_config.operator，原文）：1 等于、2 小于、3 小于等于、4 大于、5 大于等于、6 不等于、7 包含、8 不包含、9 范围查询、10 多个值匹配包含、11 多个值匹配都要包含。

机制说明：本接口按数据主题返回可用维度/指标及其互斥关系、筛选配置，供 report/custom/get 组装请求。请求示例/应答示例为页面代码卡片（Python/Java/Php/Curl），未逐字提取（见未核实清单）。

### A2. 自定义报表（report/custom/get）

- 来源：https://open.oceanengine.com/labels/7/docs/1741387668314126 （doc_id=1741387668314126，正文标题「自定义报表」）
- 请求地址：`https://api.oceanengine.com/open_api/v3.0/report/custom/get/`
- 请求方法：**GET**
- 机制原文：「自定义报表是基于巨量引擎升级版对于数据报表提供新的接口能力，您可以自由选择和组合指标和维度，以此定义数据报表的字段。」「想要获取由起量带来的增量数据，需要在筛选项中上传 field=is_boost && values=1」。维度/指标获取方式两种：调用 config/get 接口；或在巨量营销升级版平台「报表→新建/编辑自定义报表→API参数生成」生成后**手动补 data_topic**，否则无法正确获取数据。

Header：Access-Token（必填，string）。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户id |
| data_topic | 否 | string | 数据主题，**默认值 BASIC_DATA**（营销基础数据）。枚举 15 项：BASIC_DATA / QUERY_DATA / BIDWORD_DATA / MATERIAL_DATA / PRODUCT_DATA / ONE_KEY_BOOST_DATA / DMP_DATA / VIDEO_DUARATION_DATA / MATERIAL_BOOST_DATA / STD_BASIC_DATA / STD_MATERIAL_DATA / STD_QUERY_DATA / STD_BIDWORD_DATA / STD_DMP_DATA / STD_PRODUCT_DATA（注意：本页枚举**不含** STD_DPA_VIDEO_DATA，与 A1 的 16 项不同，按原文记录） |
| dimensions | 必填 | string[] | 维度列表（获取方式见机制说明） |
| metrics | 必填 | string[] | 指标列表（从前端自定义报表获取的建议不要修改） |
| filters | 否 | object[] | 过滤条件，子字段见下 |
| filters[].field | 条件必填 | string | 过滤的消耗指标字段 |
| filters[].type | 条件必填 | number | 字段类型：1 固定枚举值、2 固定输入值、3 数值类型 |
| filters[].operator | 条件必填 | number | 处理方式：1 等于、2 小于、3 小于等于、4 大于、5 大于等于、6 不等于、7 包含、8 不包含、9 范围查询、10 多个值匹配包含、11 多个值匹配都要包含 |
| filters[].values | 条件必填 | string[] | 过滤字段具体值 |
| start_time | 必填 | string | 开始时间，格式 `yyyy-MM-dd hh:mm:ss`，例 2025-07-07 00:00:00 |
| end_time | 必填 | string | 结束时间，格式同上 |
| order_by | 必填 | object[] | 排序 |
| order_by[].field | — | string | 排序字段，必须在选中的 metrics 或 dimensions 中；metrics 所有字段支持排序，dimensions 是否可排序参考维度/指标说明 |
| order_by[].type | — | string | ASC 升序 / DESC 降序 |
| page | 否 | number | 页码，默认 1 |
| page_size | 否 | number | 页面大小，默认 10，**最大 100** |

应答字段：

| 字段 | 类型 | 描述 |
|---|---|---|
| code | number | 返回码 |
| message | string | 返回信息 |
| data | object | 返回数据 |
| data.rows | object[] | 数据行 |
| data.rows[].metrics | dict[string] | 指标数据 |
| data.rows[].dimensions | dict[string] | 维度数据 |
| data.total_metrics | dict[string] | 指标汇总数据 |
| data.page_info | object | 分页信息 |
| data.page_info.page | number | 页码 |
| data.page_info.page_size | number | 页面大小 |
| data.page_info.total_number | number | 总数 |
| data.page_info.total_page | number | 总页数 |
| request_id | string | 请求日志id，唯一标识一个请求 |

### A3. SPI 全域项目报表实时数据变动消息推送（智擎版）

- 来源：https://open.oceanengine.com/labels/7/docs/1870597110326348 （doc_id=1870597110326348，正文标题「【智擎版】全域项目报表实时数据变动消息推送」）
- 回调地址：订阅任务设置的回调地址（开发者提供）；请求方式 **POST**
- 触发机制原文：「根据创建订阅任务时设置的触发规则条件，订阅账户下存在触发事件时则会根据设定间隔进行推送（创建任务时无间隔设置提醒则为实时变动推送）」
- 限制原文：「当前SPI仅支持订阅全域-推商品，暂不支持全域-推直播项目」
- 关联 API：report/custom/get（文档内链接 https://open.oceanengine.com/docs/1741387668314126 ）

请求 Header：

| 字段 | 类型 | 描述 |
|---|---|---|
| Content-Type | string | 允许值 application/json |
| X-Open-Signature | string | 签名，验证请求合法性，算法见【X-Open-Signature签名校验】（A5.2） |

推送封套（基础信息）：

| 字段 | 类型 | 描述 |
|---|---|---|
| message_id | string | 消息ID，唯一标识 |
| subscribe_task_id | number | 订阅任务ID |
| advertiser_ids | number[] | 本服务类型一次只返回一个投放账户id |
| publish_time | number | 消息实际产生时间，毫秒时间戳 |
| timestamp | number | 毫秒时间戳，本条消息实际推送时间；获取推送数据列表时无该参数 |
| nonce | number | 随机数，和 timestamp 组合防重放；获取推送数据列表时无该参数 |
| account_relation | object | 账号对应关系，包含所有授权关系 |
| account_relation.core_user_ids | number[] | 消息下发的投放账户是被哪些用户授权的 |
| service_label | string | 订阅服务类型，枚举值：**report.uniproject.activeprogram** |
| data | object | 推送数据信息，服务类型不同结构不同 |
| data.user_id | number | 触发事件的账户ID，营销场景即投放账户ID |
| data.event | string | 子事件类型，枚举值：**PRODUCT**（全域-推商品） |
| data.content | object | 事件内容 |
| data.content.project_ids | number[] | 聚合周期内发生指标变更的项目列表 |

### A4. SPI 全域基础素材报表实时数据变动消息推送（智擎版）

- 来源：https://open.oceanengine.com/labels/7/docs/1870605477567616 （doc_id=1870605477567616，正文标题「【智擎版】全域基础素材报表实时数据变动消息推送」）
- 回调地址/请求方式/触发机制/限制/关联API/Header：与 A3 完全相同（POST、application/json、X-Open-Signature、仅支持全域-推商品）。

推送封套：与 A3 相同，唯一差异：

| 字段 | 值 |
|---|---|
| service_label | **report.unimaterial.activeprogram** |
| data.event | PRODUCT（全域-推商品） |
| data.content.project_ids | number[]，「聚合周期内发生指标变更的项目列表」（**原文如此**：素材页此处仍写 project_ids；是否返回 material_ids 未在本页核实，见未核实清单。订阅服务类型总页中 report.material.activeprogram 的 content 为 material_ids） |

### A5. SPI 总览（正文标题「SPI订阅」）及验签/回调/服务类型/补拉

- 来源：https://open.oceanengine.com/labels/7/docs/1696710547608591 （doc_id=1696710547608591，正文标题「SPI订阅」）

#### A5.1 订阅机制（总览页原文要点）

- 订阅服务：提供主动推送信息的接口；订阅方式分「消息/数据」；以任务形式创建与使用。
- 优势原文：数据生产完成的消息经 API 网关主动调用，「可以减少绝大多数的无效调用，减少对于接口调用频控的浪费」。
- 订阅人：面向所有开发者，需认证完成且应用创建完成（依赖应用基础与授权基础）。
- 可订阅内容：获得客户授权的投放账户下所有在数据报表中披露的数据。
- 推送失败机制原文：「此处指消息，因为数据需要开发者主动通过API网关获取，消息发送策略规则可以在创建订阅的时候在一定选项中选择，包括**超时时间，重试间隔，最大重试次数**」（具体默认数值未在本页给出 → 未核实）。
- 配额原文：「每个应用下（APPID）针对同一个订阅服务类型，最多创建10个订阅任务。」
- 订阅账号须在 API 已授权账号范围内；客户账号/代理商/旧版工作台账号下管理客户的变更无需二次修改订阅任务；订阅账号取消授权则不再推送该账号数据，建议手动勾掉。
- 鉴权原文：「SPI的鉴权依赖于API token，如果api接口对某一个adv账号的授权过期，将不会产生推送」。
- 流程：① 开发者订阅并配置推送规则 → ② SPI 网关向预留 http 地址推送消息/数据（失败与重试依据创建订阅时的基础规则：订阅规则、策略规则、通知规则）→ ③ 收到「数据就绪」消息后主动调 API 拉取数据（可选）；若直接推送的是数据则无需重复调 API。

#### A5.2 X-Open-Signature 签名校验（验签算法原文）

- 来源：同目录子文档 doc_id=1722481732805646「X-Open-Signature签名校验」（由 SPI 推送页 Header 描述指向）。
- 机制原文：「系统会在回调请求的header中带入X-Open-Signature，该字段值是由创建订阅任务时配置的 secret_key 计算得出」；「secret_key为创建订阅任务时设置的验证密钥，可通过开发者管理后台-订阅服务-订阅任务详情进入查看」。
- **算法（官方示例代码还原，三种语言一致）：HMAC-SHA256，密钥 = secret_key 的字节，消息 = 回调请求 body 的原始字节（整个请求体，不做字段拼接/排序），输出 = 小写十六进制字符串；与 header `X-Open-Signature` 的值做字符串相等比较。**
  - Java（AuthTokenUtil.java 原文关键行）：
    ```java
    // 根据SecretKey计算签名
    public static String genSignature(String secretKey, byte[] binaryparams){
        HashFunction function = Hashing.hmacSha256(secretKey.getBytes());
        HashCode code = function.hashBytes(binaryparams);
        return code.toString();   // Guava HashCode.toString() = 小写hex
    }
    // 校验签名：isValidToken(secretKey, cacher, signature)
    //   reSignature = genSignature(secretKey, readAsBytes(cacher));
    //   return signature.equals(reSignature);
    ```
  - Golang（auth_token_util 原文关键行）：
    ```go
    hash := hmac.New(sha256.New, []byte(u.SecretKey))
    hash.Write(body)
    hashString := hex.EncodeToString(hash.Sum(nil))
    // IsValidToken: reSignature == string(signature)
    ```
  - PHP：`return hash_hmac('sha256', $body, $this->secret_key);`
- 签名串拼接顺序：**示例中不存在 timestamp/nonce/secret 的字符串拼接**——签名对象就是原始 body 字节。timestamp+nonce 的防重放是封套层面的约定（见 A3），未参与签名计算（按示例代码）。
- header 名：`X-Open-Signature`（另有 `Content-Type: application/json`）。
- 调用建议原文：「调用 AuthTokenUtil.isValidToken 进行鉴权验证签名，失败则抛出异常或catch异常业务进行处理」。

#### A5.3 SPI 回调地址设置（challenge 验证与应答要求）

- 来源：子文档 doc_id=1722470671604747「SPI回调地址设置」。
- 通信协议：HTTP/HTTPS；回调地址需要**同时支持 GET 和 POST**。
- 本页结构仅为「创建订阅时验证回调地址」的应答要求；实际推送数据格式见【订阅服务类型】。
- Challenge 验证请求参数：

| 字段 | 类型 | 描述 |
|---|---|---|
| challenge | number | 随机数 |
| event | string | 事件值，**verify_webhook** 为 Challenge 验证事件 |

- 应答要求：response 的 Content-Type 必须为 application/json，结构：

| 字段 | 类型 | 描述 |
|---|---|---|
| BaseResp | object | — |
| BaseResp.StatusCode | number | 执行状态码，**200 成功，4xx 业务异常不重试，5xx 系统异常重试** |
| BaseResp.StatusMessage | string | 执行结果 |
| challenge | number | 回传请求中的 challenge 值 |

#### A5.4 订阅服务类型（封套通用定义与服务类型清单）

- 来源：子文档 doc_id=1722468664901647「订阅服务类型」。
- 请求方式 POST；Header：Content-Type: application/json、X-Open-Signature。
- 通用封套：message_id(string)、subscribe_task_id(number)、advertiser_ids(number[]，report.advertiser.beforeday / report.advertiser.activeprogram 返回所有已产出客户ID组，其余服务类型一次仅一个)、account_relation{cc_ids 旧版工作台账号 / agent_ids 代理商授权 / advertiser_ids 客户直接授权 / core_user_ids 用户授权账号}、service_label(string)、publish_time(number 毫秒)、timestamp(number 毫秒，补拉时无)、nonce(number，补拉时无)、**data(json string)**（注意此页 data 类型为 json string，A3/A4 页写 object）。
- 服务类型清单（原文）：report.advertiser.beforeday 客户报表稳定性数据（无 data 内容，advertiser_ids 返回已产出客户ID）；change.site.realtime 落地页事件推送；action.quota.realtime 在投计划配额；status.promotion.realtime 营销状态实时推送；status.project.realtime 项目状态实时推送；report.project.activeprogram 项目报表实时数据；report.promotion.activeprogram 营销报表实时数据；report.material.activeprogram 素材报表实时数据；status.qualification.ad.realtime 资质状态推送；status.material.ad.realtime 素材状态推送（仅素材新增/删除）；settings.promotion.realtime 营销配置实时变更；settings.project.realtime 项目配置实时变更；agent_reject_promotion 代理商营销违规信息；adv.violation.score 违规积分事件；adv.disposal.score 积分处置事件；adv.reject_material.ai_repair 拒审素材修复成功事件；compensate.status.realtime 项目/营销成本保障状态变动。
- 报表类 data 内容：report.project.activeprogram → {app_id, core_user_id, project_ids}；report.promotion.activeprogram → {app_id, core_user_id, promotion_ids}；report.material.activeprogram → {app_id, core_user_id, material_ids}（聚合周期内发生指标变更的列表）。
- 状态类事件枚举（节选原文）：status.promotion.realtime 的 event 含 PROMOTION_STATUS_CHANGE / PROMOTION_STATUS_ENABLE 投放中 / PROMOTION_STATUS_DISABLE 未投放 / AUDIT_DENY / AUDIT / REAUDIT / DISABLED / DISABLE_BY_QUOTA / PROJECT_DISABLED / NO_SCHEDULE / TIME_NO_REACH / OFFLINE_BALANCE / BALANCE_OFFLINE_BUDGET / PROJECT_OFFLINE_BUDGET / PROMOTION_OFFLINE_BUDGET / LIVE_ROOM_OFF / PRODUCT_OFFLINE 商品不可投 / AWEME_ACCOUNT_DISABLED / AWEME_ANCHOR_DISABLED / PROMOTION_STATUS_FROZEN / PROMOTION_STATUS_DONE / PROMOTION_STATUS_DELETED；content{status_first, status_second[], promotion_ids}。status.project.realtime 的 event 含 PROJECT_STATUS_CHANGE / DELETE / DONE / DISABLE / STOP / BUDGET_EXCEED / NOT_START / NO_SCHEDULE / ENABLE；content{status_first, status_second[], project_ids}。
- 配额类 event：QUOTA_WARN（账户健康预警/处罚/解除，content{message, campaign_type FEED/SEARCH, delivery_range DEFAULT/UNION, punish_type WARN/LIMIT/QUOTA, status PUNISH/RELIEVE}）、AD_STATUS_DISABLE_BY_QUOTA（超配额暂停，content{ad_ids, group_ids, promotion_ids(暂不支持)}）、AD_QUOTA_OCCUPY（占用配额）、AD_QUOTA_RELEASE（释放配额）、QUOTA_CHANGE（档位变化，content{sum_quota, sum_quota_before, campaign_type, delivery_range}）。

#### A5.5 获取推送数据列表（SPI 补拉接口）

- 来源：子文档 doc_id=1725097846184964，path=`/open_api/2/spi_task/get/`
- 请求地址：`https://ad.oceanengine.com/open_api/2/spi_task/get/`；方法 **GET**
- 机制原文：查询 **2 天内** SPI 推送给回调地址的数据（当前时间-开始时间 ≤ 2 天）；token 为**应用级 token**（APP-Access-Token）。

| 请求字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| Header APP-Access-Token | 必填 | string | 应用级 token |
| app_id | 必填 | number | 应用ID |
| subscribe_id | 必填 | number | 订阅任务ID |
| start_date | 否 | string | 默认当前时间前推 1 小时，格式 yyyy-mm-dd hh:mm:ss |
| end_date | 否 | string | 默认当前时间前推 30 分钟，格式同上 |
| status | 否 | string | ALL / FAIL / SUCCESS，默认 ALL |
| cursor | 否 | number | 游标，第一次传 0 |
| count | 否 | number | 页面数据量 |

应答：data.list[]{task_id:number 数据id, data:json string（格式同推送数据）, status:string **WAITING 待推送 / FAIL 失败 / SUCCESS 成功**}；data.cursor_info{has_more:bool, count:number, cursor:number}；request_id。

### A6. RDS 总览（正文标题「连山云RDS同步」）及交付机制

- 来源：https://open.oceanengine.com/labels/7/docs/1739047000991747 （doc_id=1739047000991747）

#### A6.1 总览页原文

- 产品简介：「连山RDS同步服务使用**云数据库**，实现将巨量和千川的营销报表等数据**直接、实时、准确地自动推送**给用户」。
- 核心功能：实时营销报表订阅（「一张表自主聚合便可实现展位、创意、计划、计划组、账号维度的报表需求」）；便捷数据运维工具（数据库控制台工具、数据传输服务 DTS、数据库公网访问 EIP）。
- 使用场景：字节渠道投放 ROI 监控；非闭环电商全链路监控；更精细的投放控制（小时累计数据驱动启停）。

#### A6.2 接入流程（子文档 doc_id=1739047518722061）

1. 创建数据库：登录开放平台后跳转 `https://ad.oceanengine.com/cloud/volc/mysql`（连山控制台）按提示创建 **MySQL**。**注意原文：数据库和私有网络务必选择北京机房，否则数据无法同步。** 磁盘预估原文：1W 条有消耗创意每周约占用 1.2G，每天约落库 0.17G 报表数据。
2. 订阅 RDS 任务：`https://open.oceanengine.com/developer/admin/rds/#/rds` 按页面提示完成订阅。
3. 访问数据：方式1 开启数据库公网访问（EIP，连山控制台选中实例）；方式2 数据库迁移 DTS（`https://ad.oceanengine.com/cloud/volc/dts`，申请公测后配置，**支持迁移公网 MySQL 与 ES**）。
4. 数据查询 SQL 示例（原文代码卡片）：
   ```sql
   SELECT advertiser_id, ad_id, sum(`show`), count(`show`)
   FROM lianshan_realtime_202208XX
   WHERE advertiser_id=17424770XXXXXXXX
     AND stat_time >= '2022-08-29 00:00:00' AND stat_time < '2022-08-30 00:00:00'
   GROUP BY advertiser_id, ad_id
   ```

#### A6.3 支持数据类型与库表契约（子文档 doc_id=1739047873846279）

- 支持数据（各指向 ad.oceanengine.com/cloud/doc/ 子文档，字段明细未抓取 → 未核实）：
  - 千川账号/计划/创意报表（cloud/doc/1756428440291336）
  - 巨量升级版账号/项目/营销报表（cloud/doc/1759155464901640）
  - 巨量升级版素材报表（cloud/doc/1762504252446784）
  - 小店随心推订单数据（cloud/doc/1769951966021644）
- **库命名**：`lianshan_rds_{订阅任务ID}`，例 lianshan_rds_1731339863784456。
- **表命名**：`lianshan_realtime_{当周第一天日期}`，例 lianshan_realtime_20220425，**按周分表**。
- 表用途/粒度/推送/频次（原文）：

| 项 | 内容 |
|---|---|
| 数据用途 | 实时账户/计划组/计划/创意小时累计报表、日累计报表等 |
| 数据粒度 | 创意 × 营销展位（creative_id × inventory） |
| 推送触发 | 任一指标发生变化才推送；指标数据没有变化不推送 |
| 更新频次 | 3min 一更新；当前小时指标数据覆盖更新 |
| 取数方法 | 创意/计划/计划组/账号分别 group by 聚合；日/周/月对聚合数据 sum；**整点后 15 分钟**可获取之前整点的稳定数据；**次日 9 点**可获取前一日持久化修正数据 |

- 通用字段结构（原文；「数据指标平铺落库」）：

| 名称 | 类型 | 说明 | 业务含义 |
|---|---|---|---|
| id | bigint | 主键id | 数据推送记录id |
| 数据指标1…n | （平铺列） | — | 各报表指标列，明细见各数据类型子文档（未核实） |
| stat_time | datetime | 数据产生时间 | 报表数据的产生时间 |
| create_time | datetime | 记录创建时间 | 数据推送的记录时间 |
| update_time | datetime | 数据更新时间 | 覆盖更新后 create_time 不变，update_time 更新至最新落库时间 |
| uid | bigint | 数据 unique id | creative_id × inventory 粒度同步记录唯一标识 |

### A7. 异步上传视频文件（file/upload_task/create）+ 轮询接口

- 来源：https://open.oceanengine.com/labels/7/docs/1801533703648324 （doc_id=1801533703648324，正文标题「异步上传视频文件」）
- 请求地址：`https://api.oceanengine.com/open_api/2/file/upload_task/create/`；方法 **POST**
- 机制原文：先将视频经连山云素材服务上传取得视频链接，再以该 url 作为 video_url 入参上传；**仅支持开发者购置连山云素材服务上传生成的 tos 链接 / 连山点播云产品的视频链接，不支持其他三方链接**。

Header：Access-Token（必填 string）；Content-Type（必填，允许值 application/json）。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| account_id | 必填 | number | 账户ID |
| account_type | 必填 | string | 账户类型，允许值：**ADVERTISER 客户、AGENT 代理商、EBP 企业版工作台** |
| filename | 必填 | string | 素材的文件名 |
| labels | 否 | string[] | 标签，每个长度 10，最大 50 个。优先为无需付费信息的素材传入「仅IAA」标签（仅IAA投放、不参与IAP）；未传默认通用素材可 IAA+IAP。该能力仅短漫剧行业白名单客户生效，可用【查询白名单能力】接口传 gray_key=R3_material 查询；未开通传标签返回错误 |
| video_url | 必填 | string | 视频 url，**最大支持上传文件大小 1000M**；支持连山云视频点播链接与连山云素材服务 tos 链接 |
| is_aigc | 否 | bool | 视频素材是否为 AIGC 生成 |
| is_guide_video | 否 | bool | 是否引导视频（仅游戏行业白名单客户可用） |
| is_need_auth | 条件必填 | bool | 是否允许生效授权保护（素材需自产）；account_type=AGENT 时有效且必填，目前只支持 true（原文「AGTENT」为笔误） |

应答：code / message / data.task_id(number 任务ID) / request_id。

**轮询接口（同目录子文档 doc_id=1801544428603604「获取异步上传视频文件结果」）**：

- 请求地址：`https://api.oceanengine.com/open_api/2/file/video/upload_task/list/`；方法 **GET**；Header Access-Token。
- 机制原文：任务处理速度取决于连山云素材服务速率及任务量级，「正常会在3分钟内完成」。

| 请求字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| account_id | 必填 | number | 账户ID |
| account_type | 必填 | string | ADVERTISER / AGENT / EBP |
| task_ids | 必填 | number[] | 任务id列表，单次最多 100 个 |

应答：data.list[]：

| 字段 | 类型 | 描述 |
|---|---|---|
| status | string | **任务处理状态枚举：PROCESS 处理中、SUCCESS 成功、FAILED 失败** |
| error_msg | string | 任务失败后返回失败信息 |
| create_time | string | 任务创建时间 |
| task_id | number | 任务id |
| video_info | object | 视频素材信息 |
| video_info.video_id | string | 视频id |
| video_info.material_id | number | 素材id |
| video_info.size | number | 文件大小 |
| video_info.video_signature | string | 文件md5值 |
| video_info.width / height | number | 视频宽/高 |
| video_info.video_url | string | 视频地址 |
| video_info.duration | double | 视频时长 |

### A8. 上传视频（file/video/ad）

- 来源：https://open.oceanengine.com/labels/7/docs/1696710600730639 （doc_id=1696710600730639，正文标题「上传视频」）
- 请求地址：`https://api.oceanengine.com/open_api/2/file/video/ad/`；方法 **POST**
- 机制原文：上传营销投放相关素材视频；「建议：对于连山url链接上传视频，请及时切换至异步上传接口」。

Header：Access-Token（必填 string）；Content-Type（必填，允许值 **multipart/form-data**）。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户ID |
| upload_type | 否 | string | 上传方式：**UPLOAD_BY_FILE 文件上传（默认）/ UPLOAD_BY_URL 网址上传** |
| video_signature | 条件必填 | string | 视频 md5 值（服务端校验）；UPLOAD_BY_FILE 有效且必填 |
| video_file | 条件必填 | file | 视频文件，允许格式 **mp4、mpeg、3gp、avi、m4v，大小 ≤500M**；UPLOAD_BY_FILE 有效且必填 |
| filename | 条件必填 | string | UPLOAD_BY_URL 有效且必填；可自定义素材名，不传默认取文件名，最长 255 字符；同一素材重复上传不会改名 |
| labels | 否 | string[] | 标签（「仅IAA」白名单规则同 A7，gray_key=R3_material） |
| video_url | 条件必填 | string | UPLOAD_BY_URL 必填；**仅支持连山云素材服务 tos 链接，不支持其他三方链接** |
| is_aigc | 否 | bool | 是否 AIGC 生成 |
| is_guide_video | 否 | bool | 是否引导视频（游戏行业白名单） |
| video_cover_id | 否 | string | 视频封面ID，可通过【获取视频素材】【获取图片素材】【获取视频智能封面】获取；素材库视频与创编主流程同一视频绑定同一封面 |

应答：data：

| 字段 | 类型 | 描述 |
|---|---|---|
| video_id | string | 视频ID |
| size | number | 视频大小 |
| width / height | number | 视频宽/高 |
| video_url | string | 视频地址 |
| duration | double | 视频时长 |
| material_id | number | 素材id（多合一报表中的素材id，一个素材唯一对应一个素材id） |
| video_signature | string | 视频md5值 |
| video_cover_id | string | 视频封面id |

外层 code / message / request_id。

### A9. 获取视频素材（file/video/get）

- 来源：https://open.oceanengine.com/labels/8/docs/1696710601820172 （doc_id=1696710601820172，正文标题「获取视频素材」）
- 请求地址：`https://api.oceanengine.com/open_api/2/file/video/get/`；方法 **GET**；Header Access-Token。
- 机制原文：获取客户创意素材库视频；**素材 URL 仅查询自己主体下素材才返回**（投放账户主体须与 APPID 开发者主体一致，否则提示「素材所属主体与开发者主体不一致无法获取URL」；第三方可在授权时申请敏感物料权限）；**video_ids / material_ids / signatures 三者只能选一个过滤**；数据量 100w 以内支持全量获取。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户ID |
| filtering | 否 | object | 视频过滤条件 |
| filtering.width / height | 否 | number | 视频宽/高 |
| filtering.ratio | 否 | float[] | 宽高比，例 [1.7,2.5]；输入 1.7 搜索 1.65–1.75（精度上下浮动 0.05） |
| filtering.video_ids | 否 | string[] | 视频ids，≤100；与 material_ids/signatures 互斥 |
| filtering.material_ids | 否 | number[] | 素材id列表（素材报表使用的id），≤100；互斥 |
| filtering.signatures | 否 | string[] | md5 值列表，≤100；互斥 |
| filtering.start_time / end_time | 否 | string | 按上传时间过滤，格式 yyyy-mm-dd，搭配使用 |
| filtering.labels | 否 | string[] | 视频标签；仅IAA素材支持仅IAA筛选 |
| filtering.source | 否 | string[] | 素材来源，详见【附录-素材来源】（枚举未抓取 → 未核实）；**大小写敏感** |
| filtering.star_author_ids | 否 | string[] | 星图达人id，仅 source=STAR 时支持，单次最多 20 个 |
| page | 否 | number | 页码，默认 1 |
| page_size | 否 | number | 页面大小，默认 20 |

应答：data.list[]：

| 字段 | 类型 | 描述 |
|---|---|---|
| id | string | 视频ID |
| size / width / height | number | 大小/宽/高 |
| url | string | 视频地址（仅同主体预览；**预览链接有效期 1 小时**） |
| format | string | 视频格式 |
| signature | string | 视频md5值 |
| poster_url | string | 视频首帧截图（同主体限制同上） |
| duration | double | 视频时长 |
| material_id | number | 素材id |
| bit_rate | number | 码率，单位 bps |
| source | string | 素材来源（大小写敏感） |
| create_time | string | 上传时间，格式 "yyyy-mm-dd HH:MM:SS" |
| filename | string | 素材文件名 |
| labels | string[] | 视频标签 |
| organization_tags | string[] | （原文无描述） |
| star_author_id | string | 星图达人ID |
| video_cover_id | string | 视频封面ID |
| cover_source | string | 封面来源：**MANUAL_SOURCE 人工自提 / FIRST_FRAME 默认抽帧或平台人工裁剪 / AI_COVER AI推荐封面** |

data.page_info{page, page_size, total_page, total_number}；外层 code / message / request_id。

### A10. 更新视频（file/video/update）

- 来源：https://open.oceanengine.com/labels/7/docs/1696710605909004 （doc_id=1696710605909004，正文标题「更新视频」）
- 请求地址：`https://api.oceanengine.com/open_api/2/file/video/update/`；方法 **POST**
- 机制原文：批量更新素材视频的名称。Header：Access-Token；Content-Type: application/json。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户ID |
| videos | 必填 | object[] | 最多 100 个 |
| videos[].video_id | 必填 | string | 视频id |
| videos[].filename | 必填 | string | 视频素材名称，最多 255 字 |
| videos[].labels | 否 | string[] | **全量更新逻辑：不传=不更新；非空数组=完全替换当前所有标签；空数组=清空**。「仅IAA」白名单规则同 A7 |
| videos[].video_cover_id | 否 | string | 视频封面ID |

应答：data.videos[]：

| 字段 | 类型 | 描述 |
|---|---|---|
| video_id | string | 视频id |
| labels | string[] | 标签 |
| status_code | number | 更新状态，**字段即将下线，请切换至 update_status**；原文枚举文案 SUCCESS 成功 / FAILURE 失败（操作视频非当前客户所有或视频不存在） |
| video_cover_id | string | 视频封面id |
| update_status | string | 更新状态：**SUCCESS 成功 / FAILURE 失败**（视频非当前客户所有、视频不存在、或视频封面查询不到元信息时可能失败） |

外层 code / message / request_id。

### A11. 获取抖音主页视频（file/video/aweme/get）

- 来源：https://open.oceanengine.com/labels/7/docs/1729982871844879 （doc_id=1729982871844879，正文标题「获取抖音主页视频」）
- 请求地址：`https://ad.oceanengine.com/open_api/2/file/video/aweme/get/`（**原文 host 为 ad.oceanengine.com**）；方法 **GET**；Header Access-Token。
- 机制原文：根据抖音号id获取其主页视频id；**仅能获取抖音主页公开视频**；page 分页最多获取前 150 个视频，超过请用 filtering.aweme_url 或游标方式。

请求参数：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| advertiser_id | 必填 | number | 客户ID |
| aweme_id | 必填 | string | 抖音号，通过【获取抖音授权关系】获取 |
| filtering | 否 | object | 检索条件 |
| filtering.aweme_url | 否 | string | 抖音视频链接（抖音端「分享→复制链接」） |
| page | 否 | number | 页码，默认 1 |
| page_size | 否 | number | 页面大小，默认 10，取值 1–100 |
| cursor | 否 | string | 游标，第一次传 0；不传/空串默认用 page+page_size。**page+page_size 与 cursor+count 为两种分页方式** |
| count | 否 | number | 页面数据量（cursor 分页用） |

应答：data.list[]：

| 字段 | 类型 | 描述 |
|---|---|---|
| item_id | number | 抖音视频ID |
| title | string | 视频标题 |
| video_id | string | 视频ID |
| aweme_play_url | string | 视频播放链接 |
| duration | float | 视频时长（秒） |
| video_cover_id | string | 视频封面ID |
| video_cover_url | string | 封面预览链接，有效期 1 小时 |
| image_mode | string | 素材类型 |
| mid | number | 素材ID |
| album_id | number | **短剧id/短剧合集id；仅当 if_album=YES 时返回** |
| if_album | string | 抖音视频是否关联短剧：**YES 是 / NO 否** |

data.page_info{page, page_size, total_number, total_page}；data.cursor_info{has_more:bool, cursor:string}（cursor+count 分页时返回）；外层 code / message / request_id。

### A12. 通用返回码（附录，doc_id=1696710760866831「返回码」）

各接口 code/message 均指向本附录。抓取到的码表（code 与 msg 完整；**前半部分中文排查建议在源文档中即编码损坏**，按可辨识内容记录）：

| code | msg（原文） | 含义/排查建议 |
|---|---|---|
| 0 | OK | 请求成功 |
| 40000 | The advertiser ID or the account information is invalid. Please check and retry. | advertiser_id 错误或账户类型不符（源文档此段中文乱码，含义据英文 msg） |
| 40000 | xx : Missing data for required field. / xx: value is required but missing | 必填字段缺失 |
| 40000 | xx: Value must be an yy / xx: Field must be set to yy | 字段类型不符 |
| 40000 | xx: Value is not nullable | 字段内容不能为空 |
| 40000 | The advertiser ID is required. Please provide it and retry. | 未传 advertiser_id（GET 用 query、POST 用 body） |
| 40002 | No permission to operate account xxx | Access_Token 下无该账户授权 |
| 40002 | advertiser does no grant xxx permission | 授权时未申请对应接口权限，需重新授权 |
| 40002 | app_secret 不匹配，无权限操作当前应用 | secret 与 app_id 不一致 |
| 40130 | 当前客户(advertiser_id)的可用QPS已达上限… | 客户级 QPS 配额按历史活跃度动态分配，不支持申报提额 |
| 40100 | 系统请求频率超限，请稍后重试（历史文案 Too many requests. Please retry in some time.） | 系统级限流（接口下所有开发者请求计数） |
| 40110 | 您的开发者账户请求频率超限，请稍后重试（历史文案 Too many requests by this developer…） | 开发者秒级 QPS 限流 |
| 40102 | access_token已过期 | 重新获取 |
| 40103 | refresh_token已过期 | refresh_token 已被使用或账号重新授权生成新 Token |
| 40104 | The access_token is empty. | 传入无效，重新获取 |
| 40107 | refresh_token无效，请传入最新的refresh_token | 同 40103 |
| 40113 | App不存在或者状态异常 | 开发者后台确认应用存在且启用 |
| 40115 | 授权码无效 / auth_code已经被使用，请重新授权 | auth_code 失效：已使用 / 超 10 分钟有效期 / 同账号重新授权生成 |
| 40118 | The API is not in allow list. Please apply for the allow list and retry. | 接口未上线或开发者无该接口权限 |
| 40119 | The developer and the advertiser do not belong to the same group. Please contact the administrator. | 敏感物料主体校验；需授权时申请敏感物料授权（授权链接拼接 material_auth=1） |
| 50000 | SYS_ERROR | 系统请求超时，可以重试 |

原文说明：「4 开头错误码均为正常业务校验报错，如参数类型不符合要求，或不符合业务要求」。

---

## B. 商品库接口（飞书手册《「升级版工作台-短剧漫剧库」操作手册》+ 其引用的开放平台文档）

### B1. 手册来源与读取结果

- Wiki：飞书《「升级版工作台-短剧漫剧库」操作手册》（内网 wiki 链接已脱敏，不入公开库）
- 读取成功：obj_type=docx，标题「「升级版工作台-短剧漫剧库」操作手册」，更新时间 2026-10-30T05:39:44Z。
- 全文读取成功（xml 格式，9065 字符）。手册元信息：PR编号 2026011578291，过审时间 2026年1月16日。
- **手册本身未直接列出商品列表/商品详情等查询 MAPI 的 path**；它引用了 3 个开放平台文档（均已抓取核实，见 B3）：
  - 升级版组织-新建通用版商品：https://open.oceanengine.com/labels/7/docs/1846200859217482
  - 升级版组织-编辑通用版商品：https://open.oceanengine.com/labels/7/docs/1846200800780617
  - 短剧漫剧库类目传参：https://open.oceanengine.com/labels/7/docs/1814865185951753
- 查询类接口 path 来自开放平台 label 7 文档目录树（`/skiff/api/doc/client/tree/get/`）及对应正文（已抓取）。

### B2. 升级版库与旧版库区别（手册原文表格）

| 方向 | 旧短剧库 | 新「短剧漫剧库」 |
|---|---|---|
| 可用于授权AIGC生产素材的剧目范围 | 仅支持「字节小程序」剧目 | 支持「字节小程序」剧目、「端原生」剧目 |
| 商品库类型 | 分为「免费短剧库」与「付费短剧库」 | 统一为「短剧漫剧库」 |

其他手册要点（原文）：
- 短剧库定义：短剧商品合集，将短剧特征（名称、题材等）抽象成结构化数据存储，用于用户画像与模型探索。
- **无论手动创建商品还是使用 MAPI 接口创建商品，均需手动前置在平台页面内新建短剧库**（选择「短剧漫剧库」→ 输入库名称 → 选择关联主体；可选范围为升级版工作台的认证主体及关联主体，主体资质用于商品审核）。
- 手动创建商品改动点：投放载体选字节小程序/微信小程序 → 填「剧目ID」；选端原生组件 → 填「端原生专辑ID」；同时选两者 → 两个字段都填；**新库内无需再填写推广链接**；填有效 ID 后平台自动读取部分剧目信息回填。
- MAPI 改动点（手册原文）：① 公共参数 advertiser_id 传**升级版组织ID**；② 投放载体支持多选，创建后编辑**仅允许添加载体，不允许减少**；载体含字节小程序/微信小程序枚举 → 必填剧目ID；含端原生组件枚举 → 必填端原生专辑ID；③ **变现模式字段枚举值修改为「付费变现」、「营销变现」**（注意与 API 文档不一致，见 B7）。
- Q&A 原文：Mapi 创建商品入参账户ID（account_id）= **升级版工作台组织ID（非投放账户ID）**；短剧名以「短剧版权中心」上传名为准（未上传则以「抖音开放平台」为准）；商品库绑定主体对投放无影响；旧版「商品分析」模块新版开发中。
- AIGC 授权流程：创建含「端原生组件」的商品并填端原生专辑id（库自动校验专辑id与aigc授权关系）→ 版权方主动授权（版权中台【AIGC创意授权】发起授权，可批量授权多个商品库主体）或经商品库向版权方申请授权 → 巨量引擎创编 UBMAX 项目时开启「AIGC动态创意」开关（白名单功能）。

### B3. 商品库 MAPI path 清单（目录树 + 正文核实）

升级版（EBP，v3.0，host api.oceanengine.com）：

| 接口 | 方法 | path | 核实程度 |
|---|---|---|---|
| 升级版组织-新建通用版商品 | POST | /open_api/v3.0/dpa/ebp/product/create/ | 正文已抓（B4） |
| 升级版组织-编辑通用版商品 | POST | /open_api/v3.0/dpa/ebp/product/update/ | 正文已抓（B4） |
| 获取通用版商品列表 | GET | /open_api/v3.0/dpa/ebp/product/list/ | 正文已抓（B5） |
| 获取通用版商品详情 | GET | /open_api/v3.0/dpa/ebp/product/detail/get/ | 正文已抓（B5） |
| 获取商品库列表 | — | /open_api/v3.0/dpa/ebp/library/list/ | 仅目录树 path（正文未抓） |
| 获取升级版商品列表 | — | /open_api/v3.0/dpa/ebp/clue_product/list/ | 仅目录树 path |
| 获取升级版商品详情 | — | /open_api/v3.0/dpa/ebp/clue_product/get/ | 仅目录树 path |
| 升级版组织-创建/编辑升级版商品 | — | /open_api/v3.0/dpa/ebp/clue_product/save/ | 仅目录树 path |
| 删除升级版商品 | — | /open_api/v3.0/dpa/ebp/clue_product/delete/ | 仅目录树 path |
| 删除通用版商品 | — | /open_api/v3.0/dpa/ebp/product/delete/ | 仅目录树 path |
| 批量修改DPA商品状态 | — | /open_api/v3.0/dpa/ebp/product_status/batch_update/ | 仅目录树 path |
| 获取商品库DPA分类 | — | /open_api/v3.0/dpa/ebp/category/get/ | 仅目录树 path |
| 获取商品库DPA词包 | — | /open_api/v3.0/dpa/ebp/dict/get/ | 仅目录树 path |
| 查询短剧商品原片授权申请状态 | — | /open_api/v3.0/dpa/ebp/playlet/auth/get/ | 仅目录树 path |
| 上传短剧剧目（获取剧目ID） | — | /open_api/v3.0/dpa/album/create/ | 仅目录树 path（create 文档正文引用了它） |
| 设置商品库&商品共享 | — | /open_api/v3.0/tools/bp_asset_management/share/ | 仅目录树 path |
| 取消商品库&商品共享 | — | /open_api/v3.0/tools/bp_asset_management/share/cancel/ | 仅目录树 path |

旧版（v2，目录树 path，正文未抓）：获取商品库信息 /open_api/2/dpa/product/availables/；获取商品库元信息 /open_api/2/dpa/meta/get/；获取商品列表 /open_api/2/dpa/detail/get/；获取商品详情 /open_api/2/dpa/product/detail/get/；创建DPA商品（无商品id）/open_api/2/dpa/product/create/；创建/修改DPA商品（已有商品id）/open_api/2/dpa/product/update/；批量修改DPA商品状态 /open_api/2/dpa/product_status/batch_update/；删除通用版商品 /open_api/2/dpa/product/delete/；升级版商品 /open_api/2/dpa/clue_product/list/、/open_api/2/dpa/clue_product/detail/、/open_api/2/dpa/clue_product/save/、/open_api/2/dpa/clue_product/delete/；查询短剧商品原片授权申请状态 /open_api/2/dpa/playlet/auth/get/；获取商品投放条件列表（线索版）/open_api/2/dpa/asset_v2/list/。

相关辅助：查询白名单能力 /open_api/v3.0/tools/gray/get/（gray_key=R3_material，见 A7/A8 labels 说明）；升级版工作台上传视频 /open_api/v3.0/tools/ebp/video/upload/（仅目录树）。

### B4. 新建/编辑通用版商品 —— 短剧漫剧库字段（正文核实）

公共参数（create 与 update 相同，update 多一个 product_id）：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| account_id | 必填 | number | **商品库归属的升级版组织id**（create：创建的商品归属入参组织，默认下级组织账户可使用；update：商品归属的升级版组织id） |
| account_type | 必填 | string | 可选值：**EBP 升级版巨量引擎工作台组织** |
| platform_id | 必填 | number | 商品库ID |
| product_id | 必填（仅 update） | number | 商品ID |
| product_info | 必填 | object | 商品详情结构体（基础字段/落地页/品牌/商户/价格/其他，不同库类型入参有差异） |

Header：Access-Token；Content-Type: application/json。方法 POST。

**短剧漫剧库专属字段（product_info 下，原文；create 与 update 基本一致）**：

| 字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| name | 必填 | string | 短剧名，上限 50 字，不可含空格和书名号 |
| profession | — | object | 额外信息 |
| profession.ad_carrier | 必填 | list | 投放载体，多选。**枚举值：字节小程序、微信小程序、端原生组件、安卓/IOS应用**。创建后编辑仅允许添加、不允许减少。**传参格式为 JSON 序列化字符串且带转义：`"ad_carrier": "[\"字节小程序\",\"端原生组件\"]"`** |
| profession.album_id | 条件必填 | long | **剧目ID**：投放载体含字节小程序/微信小程序枚举时必填；暂无 ID 可调用「上传短剧剧目」接口上传短剧获取剧目ID |
| profession.playlet_id | 条件必填 | long | **专辑ID**：投放载体含端原生组件枚举时必填 |
| profession.copyright_owner | 必填 | string | 版权方主体名称，上限 25 字；自有版权填主体名称，分销填原版权方主体名称 |
| profession.description | 否 | string | 短剧简介，上限 50 字 |
| profession.playlet_gender | 必填 | int | 短剧男女频：**1 男频 / 2 女频 / 3 其他** |
| profession.playlet_num | 必填 | int | 剧类集数，最多 10 位数字 |
| profession.has_paid_content | 必填 | string | **变现模式，枚举值：付费变现 / 流量变现**（API 文档原文；手册写「付费变现/营销变现」，见 B7） |
| profession.playlet_duration | 必填 | int | 单集剧集时长（分钟） |
| profession.start_pay_playlet（create 页原文） | 必填 | int | 起始解锁集，正整数最多 10 位 |
| profession.start_pay_episode（update 页原文） | 必填 | int | 起始解锁集，正整数最多 10 位（**两页字段名不同，按原文记录**） |
| profession.max_charge | 否 | double | 最高充值档位（元），最大 999.99，两位小数 |
| profession.min_charge | 否 | double | 最低充值档位（元），同上 |
| profession.recommended_charge | 否 | double | 推荐充值档位（元），同上 |
| price_info.price | 必填 | double | 全集价格（不含赠送），最大 99.99，两位小数 |

应答：create → data.product_id(number) + data.warning(string) + request_id；update → data.product_id(number 商品ID) + request_id；外层 code/message。

其他库类型字段（同页抓到，供参考）：通用库/电商库等需 name(<150)、image_url 必填、三级分类（除通用/房产/家装/招聘/电商/引流电商/内容库外必填）、profession.brand_type_choose（标准品牌/自定义品牌/无品牌）；引流电商库 profession.platform_v2（淘宝/拼多多/京东/唯品会/阿里健康/微信小程序/其他，数组 JSON 序列化字符串多选）；小说库 profession{chapter, novel_gender 男频书/女频书/其他, novel_author, char_count 1w-10w/10w-20w/20w+, monetization_mode 付费变现/混合, copyright_owner, online_earning 是/否, pay_start_chapter, ad_start_chapter, chapter_price, vip_type 终身/年度/月度/周天/无, max_price_level, min_price_level, suggest_price_level, micro_app_link 字节小程序投放链接}。

### B5. 商品列表/详情查询契约与可投状态枚举（正文核实）

#### 获取通用版商品列表 GET /open_api/v3.0/dpa/ebp/product/list/

适用：归属升级版工作台的通用版商品；按商品库ID + 过滤条件查询。

| 请求字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| account_id | 必填 | number | 升级版组织id |
| account_type | 必填 | string | 账户类型（**本页「可选值:」后为空，未列出枚举 → 未核实**；详情页列 EBP） |
| product_platform_id | 必填 | number | 商品库id，可通过商品库列表接口获得 |
| filtering.dpa_categories | 否 | number[] | 商品行业分类（三级行业ID），长度限制 50，可从【获取行业列表】获取 |
| filtering.is_recommend | 否 | number | 是否系统推荐：1 只看系统推荐，0 不限，默认 0 |
| filtering.product_id | 否 | number | 商品ID精准匹配；与 product_name 同传以 ID 为准 |
| filtering.product_name | 否 | string | 商品名称模糊匹配，≤50 字 |
| filtering.status | 否 | number | **商品投放状态：0 不可投放，1 可投放** |
| filtering.audit_status | 否 | number | **商品审核状态：1 审核通过，2 审核拒绝，-1 审核中** |
| filtering.spu_id | 否 | string | 外部商品id |
| page | 否 | number | 默认 1 |
| page_size | 否 | number | 默认 20，最大 100；**page*page_size 需小于 30000** |

应答 data.page_info{page,page_size,total_number,total_page}；data.list[]（商品字段）：platform_id(number 商品库ID)、product_id(number)、name、title、titles(string[] 最多9条)、outer_id、description、**stock(number 库存状态 0 无库存/1 有库存)**、**status(number 投放状态 0 不可投/1 可投)**、**audit_status(number 1 通过/2 拒绝/-1 审核中)**、audit_reason_text(拒绝原因)、audit_hit_field(拒绝字段)、online_time/offline_time(number 十位 unix 时间戳)、image_url、videos[]{url,template_id}（小说库特有）、image_urls[]{url}、brand_info{brand_url,brand_url_mobile,brand_url_android_app,brand_url_ios_app,brand_id,brand_name,en_brand,brand_url_universal_link,brand_logo}、shop_keeper_info{shop_keeper_url_mobile,shop_keeper_url_android_app,shop_keeper_url_ios_app,shop_keeper_id,shop_keeper_name,shop_keeper_url,shop_keeper_url_universal_link,address}、landing_info{target_url,target_url_mobile,target_url_android_app,target_url_ios_app,target_url_universal_link}、first_category/sub_category/third_category 及对应 _id、price_info{value 原价,price 现价,price_unit,saving,discount,sales_promotion,down_payment,mortgage,daily_mortgage}、feature、mark(评分)、bought(购买量)、comments(评论数)、tags、label(小说库特有)、has_video(0 无/1 有)、spu_source(**YUNTU_SPU 云图spu / OUTER_PLATFORM_SPU 外部平台spu / WITHOUT_SPU 无spuID**)、spu_id、video、province/city(string[])、age(number[] 1 <18 / 2 18~23 / 3 24~30 / 4 31~40 / 5 41~49 / 6 >50)、profession(dict[string])；request_id。

#### 获取通用版商品详情 GET /open_api/v3.0/dpa/ebp/product/detail/get/

| 请求字段 | 必填 | 类型 | 描述 |
|---|---|---|---|
| account_id | 必填 | number | 账户ID |
| account_type | 必填 | string | **EBP 升级版巨量引擎工作台** |
| platform_id | 否 | number | 通用版商品库ID（商品库列表接口获取） |
| filtering.product_id | 否 | string | 商品ID精准匹配；商品库ID必须与商品ID匹配否则报错；最多 100 个 |
| page | 否 | number | 默认 1 |
| page_size | 否 | number | 默认 10，最大 100 |

应答：data.page_info（**传入 product_ids 则不返回**）；data.list[] 字段与列表页基本一致，差异（按原文）：
- **status：类型标 number，但枚举写「STATUS_OFFLINE 线下不可投放状态 / STATUS_ONLINE 线上可投放状态」**（与列表页 0/1 表述不一致，源文档如此，见 B7）。
- **audit_status：枚举写「AUDIT_STATUS_APPROVE 审核通过 / AUDIT_STATUS_INIT 审核中 / AUDIT_STATUS_REJECT 审核未通过」**（列表页为 1/2/-1）。
- online_time/offline_time 描述为格式 "YYYY-MM-DD"（类型仍标 number）。
- videos[].url 描述为「视频ID」（列表页为视频链接url）。
- price_info.down_payment 描述「月付」、mortgage 描述「首付」（与列表页/创建页相反，源文档如此）。

### B6. 类目传参（doc_id=1814865185951753「商品库管理相关」，正文核实）

通用版商品库行业类型（枚举，节选短剧相关）：**PLAYLET = IAP短剧库；IAAPLAYLET = IAA短剧库**；其他：GENERAL 通用、GAME 游戏、NOVEL 小说、ECOMMERCE 电商、ECOMMERCE_V2 引流电商、LIVE 直播社交、EDUCATION 教育、VIDEO 视频咨询 等（完整 40 余项已抓，存 doc14.txt）。

通用版商品类目（短剧漫剧相关，三级 ID 原文）：
- **付费短剧库/短剧漫剧库**：一级「短剧」ID **2019**（二级：都市 201901 / 悬疑 201902 / 现言 201903 / 古言 201904 / 军事 201905 / 玄幻 201906 / 其他剧情 201912；三级例：神医 20190121、赘婿 20190122、战神 20190124、甜宠 20190310、总裁 20190312、宫斗宅斗 20190408、穿越 20190410、奇幻仙侠 20190605 …）；一级「漫剧」ID **2039**（二级：都市 203901 / 悬疑 203902 / 现言 203903 / 古言 203904 / 军事 203905 / 玄幻 203906 / 其他剧情 203907；三级例：神医 20390101、赘婿 20390102 …）。
- **免费短剧库**：一级「单剧目-短剧」ID **2038**、「单剧目-漫剧」ID **2039**、「多剧目」ID **2037**（多剧目 203701 / 20370101）。
- 升级版商品类目（9 位 ID 体系）：电商店铺商品 140000000、活动 250000000、房地产 130000000、游戏 260000000、影视 290000000（电视剧 290100000 / 电影 290200000 / 综艺 290300000 / 动漫 290400000 / 少儿 290500000 / 纪录片 290600000 / 体育 290700000 / 影视剪辑 290800000 / 影视解说 290900000 / 影视推荐 291000000 / 影视资讯 291100000 / 其他 291200000）等，完整三级表已抓（doc14.txt）。

### B7. 源间不一致记录（均按原文保留，不做裁决）

1. **变现模式枚举**：飞书手册写「付费变现、营销变现」；API 文档（create/update）has_paid_content 写「付费变现、流量变现」。
2. **剧目/专辑字段命名**：API 文档中 `album_id`=剧目ID（小程序载体）、`playlet_id`=专辑ID（端原生载体）；手册截图 alt 文本出现的字段名（name、ad_carrier、album_id、play_id、copyright_owner）中「play_id」与 API 实际字段不符，且 alt 未区分语义——**以 API 文档为准**。
3. **起始解锁集字段名**：create 页 `start_pay_playlet`，update 页 `start_pay_episode`。
4. **商品可投状态**：列表页 status 为 number 0/1；详情页 status 类型标 number 但枚举名为 STATUS_OFFLINE/STATUS_ONLINE。审核状态同理（1/2/-1 vs AUDIT_STATUS_*）。
5. **data_topics 枚举**：config/get 16 项（含 STD_DPA_VIDEO_DATA）；custom/get 15 项（不含）。
6. **全域素材 SPI 的 content**：专页写 project_ids；订阅服务类型总页 report.material.activeprogram 写 material_ids。
7. **SPI 封套 data 类型**：专页（A3/A4）标 object；订阅服务类型总页标 json string。
8. 详情页 price_info.down_payment/mortgage 的中文描述与列表页/创建页互换。

---

## 未核实清单

1. **A 部分 11 个页面的 WebFetch 直抓**：全部仅得 JS 空壳（标题「API接口 - 商业开放平台」）。正文改经文档站自身 skiff JSON 接口抓取成功（见文首方法说明）；若以「页面 HTML 直抓」为标准，则直抓未成功。
2. **各接口请求示例/应答示例代码卡片**（Python/Java/Php/Curl 标签页）：除签名页（A5.2）与 RDS SQL（A6.2）外未逐字提取。
3. **SPI 重试策略默认值**：总览仅说创建订阅时可选「超时时间、重试间隔、最大重试次数」，具体默认数值与上限未抓到；推送应答的超时秒数约定未抓到（仅有 StatusCode 200/4xx/5xx 语义）。
4. **RDS 各报表类型的指标字段明细**：千川账号/计划/创意、巨量升级版账号/项目/营销、升级版素材、小店随心推订单的字段表在 ad.oceanengine.com/cloud/doc/{1756428440291336,1759155464901640,1762504252446784,1769951966021644}，未抓取；仅抓到通用字段结构（id/stat_time/create_time/update_time/uid + 指标平铺）。
5. **获取通用版商品列表 account_type 枚举**：源页「可选值:」后为空（详情页列 EBP）。
6. **返回码附录前半段中文排查建议**（0/40000/40002/40100/40110/40130 段）：源文档本身编码损坏且不可逆恢复；code 与英文 msg 完整。
7. **【附录-素材来源】枚举**（file/video/get 的 source 取值）：未抓取。
8. **B3 中标「仅目录树 path」的接口**：path 来自官方目录树（真实），但请求/应答字段正文未抓取。
9. **飞书手册截图内容**：仅 alt 描述文本，图片本体未读取；alt 中的字段名与 API 文档存在出入（见 B7.2）。
10. **全域素材 SPI 是否返回 material_ids**：专页 content 仅列 project_ids（按原文记录），未在该页核实 material_ids。
11. **report/custom/get 的 STD_DPA_VIDEO_DATA 支持性**：该页枚举未列此值（config/get 有），未进一步核实。
12. **飞书手册引用的「升级版工作台-商品库」手册**（cite doc-id=TCPWd0MeUo8KdHxFtuacX61ZnDb）与「开通手册」（JOQId0BO1oLwBFxIcBdcA0nTnic）：未读取（超出本次任务范围）。
