# 巨量引擎开放平台 · 全域投放项目接口契约（项目域）

> 采集日期：2026-11-17。来源：巨量引擎商业开放平台官方文档（open.oceanengine.com）。
> 本文件只记录官方文档中真实出现的内容；字段名、类型、枚举、描述均照录原文（包括文档中的原始拼写，
> 如 `material_status_frist`、`AUTHRIZED`、`AUTHOR_REUQUEST_PENDING_EXPIRE` 等），不做「纠正」。
>
> **采集方法**：16 个文档 URL 用 WebFetch 直接抓取均只得到 JS 外壳（含 `?origin=left_nav` 重试），
> 随后改用页面自身调用的官方文档数据接口取得同源内容：
> - 结构化契约：`https://open.oceanengine.com/skiff/api/doc/client/schema/markdown/get/?doc_id=<id>`
>   （即页面 abstract 中官方给出的 schema_url；含 method/path/请求/应答字段树、必填标记、枚举）
> - 页面正文：`https://open.oceanengine.com/skiff/api/doc/client/node/get/?language=CHINESE&doc_id=<id>&identify_key=<key>`
>   （渲染后的文档正文，含首行语义说明、限制条款、Header 表）
>
> **表格来源约定**：字段表按官方结构化 schema 渲染（嵌套字段用 `父.子` / `父[].子` 表示）；
> 「备注」摘录页面正文原文要点。个别字段的结构化 enum 数组与页面正文/描述原文拼写不一致时
> （share_type、material/create 的 image_mode、match_type 类型），两处原文均已照录并在备注中标明。
> 请求/应答示例代码区为动态加载，未抓取（见未核实清单）。


## /open_api/v3.0/uni_project/create/

**新建全域投放项目**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/uni_project/create/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854206019149016>（doc_id=1854206019149016）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| name | string | 是 | — | 项目名称，长度是1-50个字（两个英文字符占1个字） |
| delivery_product | string | 是 | ALBUM 剧目 | 营销产品，可选值:<br>ALBUM 剧目 |
| related_product | object | 否 | — | 关联产品投放相关参数 |
| related_product.product_platform_id | number(int64) | 条件必填 | — | 当营销产品=剧目时，有效且必填<br>商品库ID，可通过 【商品投放-查询商品库】 查询。 |
| related_product.product_id | number(int64) | 条件必填 | — | 商品id，可通过 【商品投放-获取商品列表】 查询。<br>当营销产品=剧目时，有效且必填 ， 单次 请求最多支持传入 一个商品id |
| aweme_id | string | 条件必填 | — | 抖音号id，可通过【获取全域创编可用抖音号】接口获取<br>当投放载体=抖音号时，有效且必填，单次请求最多支持传入一个抖音号id |
| budget | number(float) | 是 | — | 项目预算，单位为元<br>填写限制：<br>取值范围[300，9999999.99]，小数点后两位 |
| uni_roi_goal | number(float) | 是 | — | ROI系数<br>取值范围[0，5] |
| monetization_mode | string | 是 | IAA 广告变现；IAA_AND_IAP 混合变现；IAP 用户付费 | 投放变现模式，允许值：<br>IAP 用户付费<br>IAA_AND_IAP 混合变现 |
| schedule_type | string | 是 | SCHEDULE_FROM_NOW；SCHEDULE_START_END | 投放时间类型，允许值：<br>SCHEDULE_FROM_NOW 从今天起长期投放<br>SCHEDULE_START_END 设置开始和结束日期 |
| start_time | string(date) | 条件必填 | — | 投放起始时间，当schedule_type = SCHEDULE_START_END 时必填<br>如：2017-01-01 精确到天。 |
| end_time | string(date) | 条件必填 | — | 投放结束时间，当schedule_type = SCHEDULE_START_END 时必填<br>如：2017-01-01 精确到天 |
| schedule_time | string | 否 | — | 投放时段，默认全时段投放，格式是48*7位字符串，且都是0或1。也就是以半个小时为最小粒度，周一至周日每天分为48个区段，0为不投放，1为投放，不传、全传0、全传1均代表全时段投放。<br>例如：000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000，则投放时段为周一到周日的11:30~13:30 |
| aigc_dynamic_creative_switch | string | 否 | ON 开启；OFF 关闭 | 是否开启AIGC动态创意，系统将根据创意生态要求生成素材探索，不传表示开启，创建后支持修改。可选值：<br>ON 开启（默认值）<br>OFF 关闭 |
| audience | object | 否 | — | 定向设置 |
| audience.district | string | 否 | BUSINESS_DISTRICT；NONE；REGION | 地域类型，允许值:<br>BUSINESS_DISTRICT 商圈<br>REGION 行政区域<br>NONE 不限 |
| audience.geolocation | object[] | 否 | — | 从地图添加(地图位置)，district为 "BUSINESS_DISTRICT" 时填写，最多允许添加1000个 |
| audience.geolocation[].name | string | 否 | — | 地点名称 |
| audience.geolocation[].long | number(float) | 否 | — | 经度 |
| audience.geolocation[].lat | number(float) | 否 | — | 纬度 |
| audience.geolocation[].radius | number(int64) | 否 | — | 半径，单位为m，允许值为：3000～30000，默认值为6000 |
| audience.region_version | string | 否 | — | 当前仅允许传入 2.3.2 枚举，否则接口会报错<br>行政区域版本号，通过 【获取行政信息】 接口获取district = REGION 时必填 |
| audience.city | number[] | 条件必填 | — | 地域定向省市或者区县列表(当传递省份ID时,旗下市县ID可省略不传)，当district= REGION / OVERSEA 时必填，通过 【获取行政信息】 接口获取 |
| audience.location_type | string | 条件必填 | ALL；CURRENT；HOME；TRAVEL | 位置类型，允许值：<br>CURRENT 正在该地区的用户<br>HOME 居住在该地区的用户<br>TRAVEL 到该地区旅行的用户<br>ALL 该地区内的所有用户<br>当city和district有值时，有效且必填 |
| audience.gender | string | 否 | GENDER_FEMALE；GENDER_MALE；NONE 未知 | 性别， 详见 【附录-受众性别】 ，允许值：<br>GENDER_FEMALE 女<br>GENDER_MALE 男<br>NONE 不限（默认值） |
| audience.age | string[] | 否 | — | 两套枚举并存，但不支持同时传入<br>AGE_BETWEEN_18_19 , AGE_BETWEEN_20_23 , AGE_BETWEEN_24_30 , AGE_BETWEEN_31_35 , AGE_BETWEEN_36_40 , AGE_BETWEEN_41_45 , AGE_BETWEEN_46_50 , AGE_BETWEEN_51_55 , AGE_BETWEEN_56_59 , AGE_ABOVE_60<br>年龄， 详见 【附录-受众年龄区间】 允许值： AGE_BETWEEN_18_23 , AGE_BETWEEN_24_30 , AGE_BETWEEN_31_40 , AGE_BETWEEN_41_49 , AGE_ABOVE_50 或： |
| audience.platform | string[] | 否 | — | 允许值： ANDROID 、 IOS |
| audience.hide_if_converted | string | 否 | ADVERTISER；CUSTOMER；NO_EXCLUDE；ORGANIZATION；PROJECT | 过滤已转化用户，可以避免该单元再次投放给已转化过的用户。<br>允许值：<br>NO_EXCLUDE 不限制<br>PROJECT 项目<br>ADVERTISER 投放账户<br>CUSTOMER 客户<br>ORGANIZATION 组织 |
| audience.converted_time_duration | string | 否 | NONE；ONE_MONTH；SEVEN_DAY；SIX_MONTH；THREE_MONTH；TODAY；TWELVE_MONTH | 当 hide_if_converted = CUSTOMER 公司帐户 或 ORGANIZATION 组织账户时传入有效，默认为 THREE_MONTH<br>过滤时间范围，详见 【附录-过滤时间范围】 |
| audience.retargeting_tags_exclude | number[] | 否 | — | 排除人群包列表（自定义人群），内容为人群包id。人群包id可通过【 人群包列表 】获取 |
| internal_advertiser_info | object | 否 | — |  |
| internal_advertiser_info.classify | string | 否 | CLASSIFY_APPORTION 配量；CLASSIFY_EXCHANGE 换量；CLASSIFY_INTERNAL 内广；CLASSIFY_SALE 售卖；CLASSIFY_SUPPLEMENT 补量 | 可选值:<br>CLASSIFY_APPORTION 配量<br>CLASSIFY_EXCHANGE 换量<br>CLASSIFY_INTERNAL 内广<br>CLASSIFY_SALE 售卖<br>CLASSIFY_SUPPLEMENT 补量 |
| internal_advertiser_info.resource_entry_number | string | 否 | — |  |
| internal_advertiser_info.item_number | string | 否 | — |  |
| internal_advertiser_info.advertising_volume | number(int64) | 否 | — |  |
| brand_info | object | 条件必填 | — | 品牌信息， 以下参数若有则生效必填 ， 取值来源 （更新） ： 获取账户可用品牌行业信息 、 获取账户可投品牌列表 （您可通过 【查询白名单能力】 接口传入gray_key = R3_brand 查询您的账户是否具备品牌信息归一化能力。未开通权限的账户请通过 【获取关联云图的投放账户信息】 获取品牌信息后传入，否则将拦截报错。） |
| brand_info.yuntu_category_id | number(int64) | 否 | — | 品牌分类id（取自接口【 获取账户可投行业信息 】末级行业id， 有则生效必填 ） |
| brand_info.cdp_brand_id | number(int64) | 否 | — | 归一化品牌id（原cdp品牌id， 取自接口 【 获取账户可投品牌列表 】merge_brand_id ， 有则生效必填 ） |
| brand_info.ecom_brand_id | number(int64) | 否 | — | 电商品牌id |
| brand_info.brand_name_id | number(int64) | 否 | — | 云图品牌id（ 取自接口【 获取账户可投品牌列表 】yuntu_brand_detail.outer_brand_id ， 有则生效必填 ） |
| brand_info.cdp_brand_name | string | 否 | — | 归一化品牌名称（原cdp品牌名称， 取自接口【 获取账户可投品牌列表 】merge_brand_name ， 有则生效必填 ） |
| brand_info.sub_brand_names | string[] | 否 | — | 子品牌名称（ 取自接口【 获取账户可投品牌列表 】sub_brand_details.brand_full_name ， 有则生效必填 ） |
| brand_info.sub_brand_name_ids | string[] | 否 | — | 子品牌id（ 取自接口【 获取账户可投品牌列表 】sub_brand_details.outer_brand_id ， 有则生效必填 ） |
| source | string | 是 | — | 自定义抖音名称 |
| video_material_list | object[] | 否 | — | 视频素材信息，单剧目在创建时最多设置视频素材 200 个<br>如需上传更多视频素材，可通过【批量添加项目下素材】接口追投 |
| video_material_list[].image_mode | string | 条件必填 | CREATIVE_IMAGE_MODE_VIDEO 视频；CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 抖音&火山竖版视频素材 | 视频素材类型，允许值：<br>CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 竖版视频<br>CREATIVE_IMAGE_MODE_VIDEO 横版视频 |
| video_material_list[].video_id | string | 条件必填 | — | 视频ID，可通过 【获取视频素材】 或【获取账户可用的组织视频列表】获得 |
| video_material_list[].video_cover_id | string | 条件必填 | — | 视频封面图片ID，可通过 【获取视频素材】 【获取图片素材】 【获取视频智能封面】 接口获取。<br>（视频封面与视频绑定能力全新上线， 素材库视频与创编主流程使用的同一视频素材将对应绑定同一个视频封面，即 您无需填写该字段， 传入即为人工自提- 更改封面图片绑定关系。） |
| video_material_list[].item_id | number(int64) | 否 | — | 注：当aweme_id传入时有效<br>抖音短视频ID，通过 【获取抖音主页视频】 或 【获取抖音授权关系】 获取主页视频或合作授权视频 |
| video_material_list[].video_hp_visibility | string | 否 | ALWAYS_VISIBLE 主页始终可见；HIDE_VIDEO_ON_HP 主页隐藏 | 视频素材主页可见性设置，该参数只针对非抖音视频生效可选值:<br>ALWAYS_VISIBLE 主页始终可见<br>HIDE_VIDEO_ON_HP 仅单次展示可见（默认值） |
| playlet_series_url_info | object[] | 否 | — | 短剧专辑链接信息，注意：IAA链接仅支持传入1个，IAP链接最多支持传入10个 |
| playlet_series_url_info[].playlet_series_url_type | string | 条件必填 | IAA；IAP | 短剧专辑链接类型，允许值：<br>IAA ，当delivery_product=ALBUM剧目，monetization_mode=IAA_AND_IAP 时，支持传入<br>IAP ，当delivery_product=ALBUM剧目，且monetization_mode=IAP 或 monetization_mode=IAA_AND_IAP 时，支持传入 |
| playlet_series_url_info[].playlet_series_url | string | 条件必填 | — | 短剧专辑链接<br>IAA链接，当playlet_series_url_type= IAA 时，支持传入<br>IAP链接，当playlet_series_url_type= IAP 时，支持传入 |
| title_material_list | object[] | 否 | — | 标题素材信息，单剧目最多设置标题素材100个 |
| title_material_list[].title | string | 条件必填 | — | 创意标题 |
| product_info | object | 否 | — | 产品信息 |
| product_info.titles | string[] | 否 | — | 产品名称，字数限制：[1-20]，数组上限为1<br>本参数非必填，全域中所关联的产品会在此参数中，自动绑定一个与产品相关的title<br>用户可在默认绑定的titles外，额外填写1个产品名称 |
| product_info.image_ids | string[] | 否 | — | 产品主图，尺寸要求108*108，上限10个<br>可通过【获取图片素材】接口获得<br>全域中所关联的产品会在此参数中，绑定一个与产品相关主图，除了自动绑定的主图外，还需手动上传至少1个主图，上限为10个 |
| product_info.selling_points | string[] | 否 | — | 产品卖点，字数限制：[6-9]，数组上限为10 |
| call_to_action_buttons | string[] | 否 | — | 行动号召文案，字数限制：[2-6]，数组上限为10 |
| going_global_switch | string | 否 | ON；OFF | 海外流量拓展开关，允许值：<br>ON 开启<br>OFF 关闭（默认值）<br>当且仅当monetization_mode= IAA_AND_IAP 混合变现时支持传入 |
| going_global_budget | number(double) | 否 | — | 出海日预算，当且仅当going_global_switch= ON 时支持传入<br>支持范围：300 ~ 9,999,999.99 元 |
| going_global_roi_goal | number(double) | 否 | — | 出海ROI，当且仅当going_global_switch= ON 时支持传入 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.project_id | number(int64) | 项目id |
| data.error_list | object[] | 素材上传失败的明细列表 |
| data.error_list[].object_type | string | 操作对象 |
| data.error_list[].error_code | string | 错误码 |
| data.error_list[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- 请求地址（页面原文）：`https://api.oceanengine.com/open_api/v3.0/uni_project/create/`。
- **200 素材上限**：`video_material_list` 描述原文——「视频素材信息，单剧目在创建时最多设置视频素材 200 个；如需上传更多视频素材，可通过【批量添加项目下素材】接口追投」。上限写在 `video_material_list` 字段描述里，没有独立的上限字段。追投接口为 `/open_api/v3.0/oc_project/material/create/`。
- `title_material_list`：单剧目最多设置标题素材 100 个。
- `playlet_series_url_info`：IAA 链接仅支持传入 1 个，IAP 链接最多支持传入 10 个；IAA 仅当 delivery_product=ALBUM 且 monetization_mode=IAA_AND_IAP 时支持，IAP 当 monetization_mode=IAP 或 IAA_AND_IAP 时支持。
- `budget` 取值范围 [300, 9999999.99]，小数点后两位；`uni_roi_goal` 取值范围 [0, 5]。
- `brand_info` 为条件必填：「以下参数若有则生效必填」。取值来源（更新）：获取账户可用品牌行业信息、获取账户可投品牌列表。可通过【查询白名单能力】接口传入 gray_key=R3_brand 查询账户是否具备品牌信息归一化能力；未开通权限的账户须通过【获取关联云图的投放账户信息】获取品牌信息后传入，否则将拦截报错。
- `brand_info` 子字段取值映射（描述原文）：yuntu_category_id←【获取账户可投行业信息】末级行业id；cdp_brand_id←【获取账户可投品牌列表】merge_brand_id；brand_name_id←yuntu_brand_detail.outer_brand_id；cdp_brand_name←merge_brand_name；sub_brand_names←sub_brand_details.brand_full_name；sub_brand_name_ids←sub_brand_details.outer_brand_id；ecom_brand_id 为电商品牌id。
- `aweme_id`：当投放载体=抖音号时有效且必填，单次请求最多传入 1 个抖音号id；`related_product.product_id` 当营销产品=剧目时有效且必填，单次最多 1 个商品id。
- `schedule_time`：48*7 位 0/1 字符串（半小时粒度，周一至周日）；不传、全 0、全 1 均代表全时段投放。
- `aigc_dynamic_creative_switch`：不传表示开启（ON 为默认值），创建后支持修改。
- `audience.region_version`：当前仅允许传入 2.3.2 枚举，否则接口会报错；`audience.age` 两套枚举并存但不支持同时传入；`geolocation` 最多 1000 个，radius 3000~30000m 默认 6000。
- `product_info`：titles 字数 [1-20] 数组上限 1（系统自动绑定 1 个产品相关 title，可额外再填 1 个）；image_ids 尺寸 108*108、上限 10（自动绑定外还需手动上传至少 1 个）；selling_points 字数 [6-9] 上限 10；call_to_action_buttons 字数 [2-6] 上限 10。
- `going_global_switch` 仅当 monetization_mode=IAA_AND_IAP 时支持；going_global_budget / going_global_roi_goal 仅当 switch=ON 时支持，预算范围 300~9999999.99 元。
- 应答 `data.project_id` 为新建项目id；`data.error_list` 为素材上传失败的明细列表（object_type / error_code / error_message）。
- **monetization_mode 差异**：页面正文允许值仅 IAP 用户付费 / IAA_AND_IAP 混合变现；官方结构化 schema 的 enum 数组另含 `IAA 广告变现`。两处原文均照录（见字段表枚举列与说明列）。

## /open_api/v3.0/uni_project/update/

**编辑全域投放项目**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/uni_project/update/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854910852321354>（doc_id=1854910852321354）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| name | string | 否 | — | 项目名称，长度是1-50个字（两个英文字符占1个字） |
| budget | number(float) | 否 | — | 取值范围[300，999999999.99]，小数点后两位<br>填写限制：<br>项目预算，单位为元 |
| uni_roi_goal | number(float) | 否 | — | 取值范围[0，5]<br>全域ROI系数 |
| monetization_mode | string | 否 | IAA 广告变现；IAA_AND_IAP 混合变现；IAP 用户付费 | 可选值:<br>IAA_AND_IAP 混合变现<br>IAP 用户付费 |
| schedule_type | string | 否 | SCHEDULE_FROM_NOW；SCHEDULE_START_END | 投放时间类型，允许值：<br>SCHEDULE_FROM_NOW 从今天起长期投放<br>SCHEDULE_START_END 设置开始和结束日期 |
| end_time | string(date) | 否 | — | 投放结束时间，当schedule_type = SCHEDULE_START_END 时必填<br>如：2017-01-01 精确到天 |
| schedule_time | string | 否 | — | 投放时段，默认全时段投放，格式是48*7位字符串，且都是0或1。也就是以半个小时为最小粒度，周一至周日每天分为48个区段，0为不投放，1为投放，不传、全传0、全传1均代表全时段投放。<br>例如：000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000000000000000000000000001111000000000000000000000，则投放时段为周一到周日的11:30~13:30 |
| aigc_dynamic_creative_switch | string | 否 | ON 开启；OFF 关闭 | 是否开启AIGC动态创意，枚举值：<br>ON 开启<br>OFF 关闭 |
| brand_info | object | 否 | — | 品牌信息，仅关联云图的客户可以按需填写，以下参数可通过【 获取关联云图的投放账户信息 】获取<br>（ 您可通过 【查询白名单能力】 接口传入gray_key = R3_brand 查询您的账户是否具备品牌信息归一化能力。已开通权限的账户请通过 获取账户可用品牌行业信息 、 获取账户可投品牌列表 获取品牌信息后传入，且对该账户 增量项目传入ecom_brand_id将不生效 ，增量项目 可通过【 获取全域投放项目列表 】获取，若接口应答r3_brand_merge_oc字段则可判断为增量项目） |
| brand_info.yuntu_category_id | number(int64) | 否 | — | 品牌分类id |
| brand_info.cdp_brand_id | number(int64) | 否 | — | cdp品牌id |
| brand_info.ecom_brand_id | number(int64) | 否 | — | 电商品牌id |
| brand_info.brand_name_id | number(int64) | 否 | — | 云图品牌id |
| brand_info.cdp_brand_name | string | 否 | — | cdp品牌名称 |
| brand_info.sub_brand_names | string[] | 否 | — | 子品牌名称 |
| brand_info.sub_brand_name_ids | string[] | 否 | — | 子品牌id |
| source | string | 否 | — |  |
| audience | object | 否 | — | 定向设置 |
| audience.district | string | 否 | BUSINESS_DISTRICT；NONE；REGION | 地域类型，允许值:<br>BUSINESS_DISTRICT 商圈<br>REGION 行政区域<br>NONE 不限 |
| audience.geolocation | object[] | 否 | — | 从地图添加(地图位置)，district为 "BUSINESS_DISTRICT" 时填写，最多允许添加1000个 |
| audience.geolocation[].name | string | 否 | — | 地点名称 |
| audience.geolocation[].long | number(float) | 否 | — | 经度 |
| audience.geolocation[].lat | number(float) | 否 | — | 纬度 |
| audience.geolocation[].radius | number(int64) | 否 | — | 半径，单位为m，允许值为：3000～30000 |
| audience.region_version | string | 否 | — | 当前仅允许传入 2.3.2 枚举，否则接口会报错<br>行政区域版本号，通过 【获取行政信息】 接口获取district = REGION 时必填 |
| audience.city | number[] | 否 | — | 地域定向省市或者区县列表(当传递省份ID时,旗下市县ID可省略不传)，当district= REGION / OVERSEA 时必填，通过 【获取行政信息】 接口获取 |
| audience.location_type | string | 否 | ALL；CURRENT；HOME；TRAVEL | 位置类型，允许值：<br>CURRENT 正在该地区的用户<br>HOME 居住在该地区的用户<br>TRAVEL 到该地区旅行的用户<br>ALL 该地区内的所有用户 |
| audience.gender | string | 否 | GENDER_FEMALE；GENDER_MALE；NONE 未知 | 性别， 详见 【附录-受众性别】 允许值： GENDER_FEMALE 、 GENDER_MALE 、 NONE |
| audience.age | string[] | 否 | — | 两套枚举并存，但不支持同时传入<br>AGE_BETWEEN_18_19 , AGE_BETWEEN_20_23 , AGE_BETWEEN_24_30 , AGE_BETWEEN_31_35 , AGE_BETWEEN_36_40 , AGE_BETWEEN_41_45 , AGE_BETWEEN_46_50 , AGE_BETWEEN_51_55 , AGE_BETWEEN_56_59 , AGE_ABOVE_60<br>年龄， 详见 【附录-受众年龄区间】 允许值： AGE_BETWEEN_18_23 , AGE_BETWEEN_24_30 , AGE_BETWEEN_31_40 , AGE_BETWEEN_41_49 , AGE_ABOVE_50 或： |
| audience.platform | string[] | 否 | — | 允许值： ANDROID 、 IOS<br>投放平台列表 |
| audience.hide_if_converted | string | 否 | ADVERTISER；CUSTOMER；NO_EXCLUDE；ORGANIZATION；PROJECT | 过滤已转化用户，可以避免该单元再次投放给已转化过的用户。<br>允许值：<br>NO_EXCLUDE 不限制<br>PROJECT 项目<br>ADVERTISER 投放账户<br>CUSTOMER 客户<br>ORGANIZATION 组织 |
| audience.converted_time_duration | string | 否 | NONE；ONE_MONTH；SEVEN_DAY；SIX_MONTH；THREE_MONTH；TODAY；TWELVE_MONTH | 当 hide_if_converted = CUSTOMER 公司帐户 或 ORGANIZATION 组织账户时传入有效<br>过滤时间范围，详见 【附录-过滤时间范围】 |
| audience.retargeting_tags_exclude | number[] | 否 | — | 排除人群包列表（自定义人群），内容为人群包id。人群包id可通过【 人群包列表 】获取 |
| playlet_series_url_info | object[] | 否 | — | 短剧专辑链接信息 |
| playlet_series_url_info[].playlet_series_url_type | string | 否 | IAA；IAP | 短剧专辑链接类型，允许值：<br>IAA ，当delivery_product=ALBUM剧目，monetization_mode=IAA_AND_IAP 时，支持传入<br>IAP ，当delivery_product=ALBUM剧目，且monetization_mode=IAP 或 monetization_mode=IAA_AND_IAP 时，支持传入 |
| playlet_series_url_info[].playlet_series_url | string | 否 | — | IAA链接，当playlet_series_url_type= IAA 时，支持传入，仅支持传入1个，超过1个时报错<br>IAP链接，当playlet_series_url_type= IAP 时，支持传入，长度：[1，10]，超过10个时报错<br>短剧专辑链接 |
| product_info | object | 否 | — | 产品信息 |
| product_info.titles | string[] | 否 | — | 产品名称，字数限制：[1-20]，数组上限为1<br>本参数非必填，全域中所关联的产品会在此参数中，自动绑定一个与产品相关的title<br>用户可在默认绑定的titles外，额外填写1个产品名称 |
| product_info.image_ids | string[] | 否 | — | 全域中所关联的产品会在此参数中，绑定一个与产品相关主图，除了自动绑定的主图外，还需手动上传至少1个主图，上限为10个<br>产品主图，尺寸要求108*108，上限10个<br>可通过 【获取图片素材】 接口获得 |
| product_info.selling_points | string[] | 否 | — | 产品卖点，字数限制：[6-9]，数组上限为10 |
| call_to_action_buttons | string[] | 否 | — | 行动号召 |
| title_material_list | object[] | 否 | — | 标题素材信息，单剧目最多设置标题素材100个 |
| title_material_list[].title | string | 否 | — | 创意标题 |
| going_global_switch | string | 否 | ON；OFF | 海外流量拓展开关，允许值：<br>ON 开启<br>OFF 关闭<br>当且仅当monetization_mode= IAA_AND_IAP 混合变现时支持传入 |
| going_global_budget | number(double) | 否 | — | 出海日预算，当且仅当going_global_switch= ON 时支持传入<br>支持范围：300 ~ 9,999,999.99 元 |
| going_global_roi_goal | number(double) | 否 | — | 出海ROI，当且仅当going_global_switch= ON 时支持传入<br>范围 ：(0, 5] |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.project_id | number(int64) | 项目id |
| data.error_list | object[] | 错误list，项目为分块更新，存在部分内容更新失败，部分内容更新成功<br>若更新成功，ErrorList为空数组<br>若更新失败，ErrorList包含错误的模块和失败原因 |
| data.error_list[].object_type | string |  |
| data.error_list[].error_code | number(int64) | 错误码 |
| data.error_list[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- **增量语义（页面首行原文）：「接口的更新逻辑为增量更新」。** 页面未进一步解释传 null / 空字符串 / 空数组的含义（见文末未核实清单）。
- 请求字段共 20 个：相比 create 缺少 delivery_product、related_product、aweme_id、start_time、video_material_list、internal_advertiser_info——即项目营销产品、关联商品、抖音号、起始时间、视频素材不在本接口修改范围内（视频素材追投走 oc_project/material/create）。
- `budget` 本页取值范围原文为 [300, 999999999.99]（与 create 页的 [300, 9999999.99] 不一致，两处均照录各自页面原文）。
- `brand_info`：仅关联云图的客户可以按需填写。已开通品牌归一化（gray_key=R3_brand）的账户通过获取账户可用品牌行业信息、获取账户可投品牌列表取值；**对增量项目传入 ecom_brand_id 将不生效**；增量项目可通过【获取全域投放项目列表】应答的 `r3_brand_merge_oc` 字段判断。
- `playlet_series_url_info`：IAA 链接仅支持传入 1 个，超过 1 个时报错；IAP 链接长度 [1,10]，超过 10 个时报错。
- `going_global_roi_goal` 范围原文 (0, 5]。
- **应答 error_list 语义（原文）**：「错误list，项目为分块更新，存在部分内容更新失败，部分内容更新成功。若更新成功，ErrorList为空数组；若更新失败，ErrorList包含错误的模块和失败原因。」本页 error_code 类型标注为 number（create 页为 string，照录）。
- **monetization_mode 差异**：页面正文可选值仅 IAA_AND_IAP / IAP；官方结构化 schema 的 enum 数组另含 `IAA 广告变现`。两处原文均照录。

## /open_api/v3.0/uni_project_list/get/

**获取全域投放项目列表**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/uni_project_list/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854926110406984>（doc_id=1854926110406984）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| filtering | object | 否 | — | 过滤器 |
| filtering.project_id | number[] | 否 | — | 按项目ID过滤，范围为1-100 |
| filtering.name | string | 否 | — | （两个英文字符占1个字，该字段采取模糊查询的方式。）<br>项目名称，长度是1-50个字 |
| filtering.status_first | string | 否 | PROJECT_STATUS_DELETE 已删除；PROJECT_STATUS_DISABLE 未投放；PROJECT_STATUS_DONE 已完成；PROJECT_STATUS_ENABLE 启用中；PROJECT_STATUS_FROZEN 已终止 | 项目一级状态过滤，允许值：<br>PROJECT_STATUS_DELETE 已删除<br>PROJECT_STATUS_DISABLE 未投放<br>PROJECT_STATUS_DONE 已完成<br>PROJECT_STATUS_ENABLE 启用中<br>PROJECT_STATUS_FROZEN 已终止<br>不传默认返回不限（不含已删除） |
| filtering.status_second | string | 否 | ECOSYSTEM_RISK_FAILED 存在严重生态问题；PROJECT_STATUS_AUDIT 新建审核中；PROJECT_STATUS_AWEME_ACCOUNT_DISABLED 抖音账号不可投放；PROJECT_STATUS_BUDGET_EXCEED 项目超出预算；PROJECT_STATUS_NOT_START 未达投放时间；PROJECT_STATUS_NO_SCHEDULE 不在投放时段；PROJECT_STATUS_OFFLINE_AUDIT 审核不通过；PROJECT_STATUS_PRODUCT_OFFLINE 商品不可投放；PROJECT_STATUS_REAUDIT 修改审核中；PROJECT_STATUS_STOP 手动暂停 | 项目二级状态过滤，允许值：<br>ADVERTISER_AVATAR_OFFLINE 未投放-账户头像不可投<br>PROJECT_STATUS_OFFLINE_AUDIT 未投放-审核不通过<br>PROJECT_STATUS_AUDIT 未投放- 新建审核中<br>PROJECT_STATUS_REAUDIT 未投放-修改审核中<br>PROJECT_STATUS_STOP 未投放-手动暂停<br>PROJECT_STATUS_ADV_OFFLINE_BUDGET 未投放-账户余额不足<br>PROJECT_STATUS_OFFLINE_BUDGET 未投放-账户超预算<br>PROJECT_STATUS_BUDGET_EXCEED 未投放-项目超出预算<br>PROJECT_STATUS_NOT_START 未投放-未达投放时间<br>PROJECT_STATUS_REACH_DAILY_DELIVERY 未投放-已达投放时长<br>PROJECT_STATUS_NO_SCHEDULE 未投放-不在投放时段<br>PROJECT_STATUS_PRODUCT_OFFLINE 未投放-商品不可投放<br>AWEME_ACCOUNT_DISABLED 未投放-关联抖音账号不可投放 |
| filtering.project_create_start_time | string | 否 | — | 项目创建开始时间，该筛选支持您细分到秒级查询一段时间内修改过的项目信息<br>格式yyyy-mm-dd hh:mm:ss<br>注意<br>必须与结束时间project_create_end_time结合使用，注意时间跨度不能超过30天<br>开始时间<=结束时间 |
| filtering.project_create_end_time | string | 否 | — | 项目创建结束时间，该筛选支持您细分到秒级查询一段时间内修改过的项目信息<br>格式yyyy-mm-dd hh:mm:ss<br>注意：<br>必须与开始时间project_create_start_time结合使用，注意时间跨度不能超过30天<br>开始时间<=结束时间 |
| filtering.project_modify_start_time | string | 否 | — | 项目更新开始时间，该筛选支持您细分到秒级查询一段时间内修改过的项目信息<br>格式yyyy-mm-dd hh:mm:ss<br>注意：<br>必须与结束时间project_modify_end_time结合使用，注意时间跨度不能超过30天<br>开始时间<=结束时间 |
| filtering.project_modify_end_time | string | 否 | — | 项目更新结束时间，该筛选支持您细分到秒级查询一段时间内修改过的项目信息<br>格式yyyy-mm-dd hh:mm:ss<br>注意：<br>必须与开始时间project_modify_start_time结合使用，注意时间跨度不能超过30天<br>开始时间<=结束时间 |
| page | number(int64) | 否 | — | 页码，默认值：1 |
| page_size | number(int64) | 否 | — | 页面大小，允许值：10, 20, 50, 100，默认值：10 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.page_info | object | 分页信息 |
| data.page_info.page | number(int64) | 页码 |
| data.page_info.page_size | number(int64) | 页面大小 |
| data.page_info.total_number | number(int64) | 总数 |
| data.page_info.total_page | number(int64) | 总页数 |
| data.list | object[] | 项目列表 |
| data.list[].project_id | number(int64) | 项目id |
| data.list[].advertiser_id | number(int64) | 客户id |
| data.list[].name | string | 项目名称 |
| data.list[].related_product | object | 关联产品投放相关参数 |
| data.list[].related_product.product_platform_id | number(int64) | 商品库ID |
| data.list[].related_product.album_id | number(int64) | 短剧专辑id |
| data.list[].related_product.product_id | number(int64) | 商品id |
| data.list[].aweme_id | string | 抖音号id |
| data.list[].budget | number(float) | 项目预算，单位为元 |
| data.list[].uni_roi_goal | number(double) | 全域ROI系数 |
| data.list[].monetization_mode | string | 投放变现模式，允许值：<br>IAP用户付费<br>IAA_AND_IAP混合变现 |
| data.list[].schedule_type | string | 投放时间类型，允许值：<br>SCHEDULE_FROM_NOW 从今天起长期投放<br>SCHEDULE_START_END 设置开始和结束日期 |
| data.list[].start_time | string | 投放起始时间 |
| data.list[].end_time | string | 投放结束时间 |
| data.list[].schedule_time | string | 投放时段 |
| data.list[].brand_info | object | 品牌信息 |
| data.list[].brand_info.yuntu_category_id | number(int64) | 品牌分类id |
| data.list[].brand_info.cdp_brand_id | number(int64) | cdp品牌id |
| data.list[].brand_info.ecom_brand_id | number(int64) | 电商品牌id |
| data.list[].brand_info.brand_name_id | number(int64) | 云图品牌id |
| data.list[].brand_info.cdp_brand_name | string | cdp品牌名称 |
| data.list[].brand_info.sub_brand_names | string[] | 子品牌名称 |
| data.list[].brand_info.sub_brand_name_ids | string[] | 子品牌id |
| data.list[].brand_info.r3_brand_merge_oc | number(int64) | 增量项目判断。品牌归一化能力上线，可通过该字段判断是否为增量项目。<br>（您可通过 【查询白名单能力】 接口传入gray_key = R3_brand 查询您的账户是否具备品牌信息归一化能力。 且若该字段透传增量项目，则在 【编辑全域投放项目】 传入ecom_brand_id将不生效，且需在创编中传入新的品牌信息 ） |
| data.list[].aigc_dynamic_creative_switch | string | 是否开启AIGC动态创意，允许值：<br>OFF 关闭<br>ON 开启 |
| data.list[].status_first | string | 项目一级状态，可选值:<br>PROJECT_STATUS_DELETE 已删除<br>PROJECT_STATUS_DISABLE 未投放<br>PROJECT_STATUS_DONE 已完成<br>PROJECT_STATUS_ENABLE 启用中<br>PROJECT_STATUS_FROZEN 已终止 |
| data.list[].status_second | string[] | 项目二级状态，可选值:<br>PROJECT_STATUS_ADV_OFFLINE_BUDGET 账户余额不足<br>PROJECT_STATUS_AUDIT 新建审核中<br>PROJECT_STATUS_AWEME_ACCOUNT_DISABLED 抖音账号不可投放<br>PROJECT_STATUS_BUDGET_EXCEED 项目超出预算<br>PROJECT_STATUS_NOT_START 未达投放时间<br>PROJECT_STATUS_NO_SCHEDULE 不在投放时段<br>PROJECT_STATUS_OFFLINE_AUDIT 审核不通过<br>PROJECT_STATUS_OFFLINE_BUDGET 账户超预算<br>PROJECT_STATUS_PRODUCT_OFFLINE 产品不可投放<br>PROJECT_STATUS_REAUDIT 修改审核中<br>PROJECT_STATUS_STOP 已暂停 |
| data.list[].source | string | 来源 |
| data.list[].audience | object | 定向设置 |
| data.list[].audience.district | string | 地域类型允许值:<br>BUSINESS_DISTRICT 商圈<br>REGION 行政区域<br>NONE 不限 |
| data.list[].audience.geolocation | object[] | 从地图添加(地图位置) |
| data.list[].audience.geolocation[].radius | number(int64) | 半径，单位为m |
| data.list[].audience.geolocation[].name | string | 地点名称 |
| data.list[].audience.geolocation[].long | number(float) | 经度 |
| data.list[].audience.geolocation[].lat | number(float) | 纬度 |
| data.list[].audience.region_version | string | 行政区域版本号 |
| data.list[].audience.city | number[] | 地域定向省市或者区县列表 |
| data.list[].audience.location_type | string | 位置类型，可选值:<br>CURRENT 正在该地区的用户<br>HOME 居住在该地区的用户<br>TRAVEL 到该地区旅行的用户<br>ALL 该地区内的所有用户 |
| data.list[].audience.gender | string | 性别，可选值:<br>GENDER_FEMALE 女性<br>GENDER_MALE 男性<br>NONE 不限 |
| data.list[].audience.age | string[] | 年龄 |
| data.list[].audience.retargeting_tags_exclude | number[] | 排除人群包列表（自定义人群），内容为人群包id。人群包id可通过【 人群包列表 】获取 |
| data.list[].audience.platform | string[] | 投放平台列表<br>允许值： ANDROID 、 IOS |
| data.list[].audience.hide_if_converted | string | 过滤已转化用户，可选值:<br>ADVERTISER<br>CUSTOMER<br>NO_EXCLUDE<br>ORGANIZATION<br>PROJECT |
| data.list[].audience.converted_time_duration | string | 过滤时间范围，可选值:<br>NONE<br>ONE_MONTH<br>SEVEN_DAY<br>SIX_MONTH<br>THREE_MONTH<br>TODAY<br>TWELVE_MONTH |
| data.list[].pricing | string | 可选值:<br>OCPM<br>OCPM_CPS2<br>默认值: OCPM |
| data.list[].going_global_switch | string | 海外流量拓展开关，允许值：<br>ON 开启<br>OFF 关闭 |
| data.list[].going_global_budget | number(float) | 出海日预算 |
| data.list[].going_global_roi_goal | number(double) | 出海roi |
| data.list[].going_global_budget_status | string | 可选值:<br>TRUE 达限<br>FALSE 未达限 |
| request_id | string | 请求日志id |

### 备注

- `filtering.project_id` 按项目ID过滤，范围 1-100；`filtering.name` 模糊查询（两个英文字符占 1 个字，长度 1-50）。
- `filtering.status_first` 不传默认返回不限（不含已删除）。注意：filtering 中 status_second 类型为 string，应答 list 中 status_second 类型为 string[]。
- 时间过滤（project_create_start_time / project_create_end_time / project_modify_start_time / project_modify_end_time）：格式 yyyy-mm-dd hh:mm:ss，必须成对使用，时间跨度不能超过 30 天，开始时间<=结束时间。
- `page` 默认 1；`page_size` 允许值 10, 20, 50, 100，默认 10。
- 应答 `list[].brand_info.r3_brand_merge_oc`（number）：增量项目判断。品牌归一化能力上线后可通过该字段判断是否为增量项目；若透传增量项目，则【编辑全域投放项目】传入 ecom_brand_id 将不生效，且需在创编中传入新的品牌信息。
- 应答 `list[].pricing` 可选值 OCPM / OCPM_CPS2，默认 OCPM；`going_global_budget_status` 可选值 TRUE 达限 / FALSE 未达限。
- 分页：data.page_info{page, page_size, total_number, total_page}。

## /open_api/v3.0/oc_project/reject_reason/get/

**获取投放项目审核建议**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/oc_project/reject_reason/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854930234989579>（doc_id=1854930234989579）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| page | number(int64) | 否 | — | 页数默认值： 1 ，page范围为[1,99999] |
| page_size | number(int64) | 否 | — | 页面大小默认值： 10 ，page_size范围为[1,100] |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.list | object[] | 审核建议列表 |
| data.list[].project_id | number(int64) | 项目ID |
| data.list[].project_reject | object[] | 项目维度审核建议列表 |
| data.list[].project_reject[].content | string | 审核项 |
| data.list[].project_reject[].reject_reason | string[] | 拒绝建议 |
| data.list[].project_reject[].suggestion | string[] | 审核建议 |
| data.list[].material_reject | object[] | 素材维度审核建议列表 |
| data.list[].material_reject[].type | string | 素材类型 |
| data.list[].material_reject[].item | string | 根据不同素材类型返回相应的值 |
| data.list[].material_reject[].reject_reason | string[] | 拒绝建议 |
| data.list[].material_reject[].suggestion | string[] | 审核建议 |
| data.list[].material_reject[].audit_platform | string | 审核来源类型，返回值：AD 广告审核、CONTENT 内容审核 可选值:<br>AD 广告审核<br>CONTENT 内容审核 |
| data.page_info | object | 分页信息 |
| data.page_info.page | number(int64) | 页码 |
| data.page_info.page_size | number(int64) | 页面大小 |
| data.page_info.total_number | number(int64) | 总数 |
| data.page_info.total_page | number(int64) | 总页数 |
| request_id | string | 请求日志id |

### 备注

- **path 前缀为 `oc_project`（不是 uni_project）**：`/open_api/v3.0/oc_project/reject_reason/get/`。
- `page` 默认 1，范围 [1, 99999]；`page_size` 默认 10，范围 [1, 100]。
- 应答 `data.list[]` 含项目维度审核建议 `project_reject[]`（content 审核项 / reject_reason[] 拒绝建议 / suggestion[] 审核建议）与素材维度审核建议 `material_reject[]`（type 素材类型 / item 按素材类型返回相应值 / reject_reason[] / suggestion[] / audit_platform）。
- `audit_platform` 审核来源类型：AD 广告审核、CONTENT 内容审核。

## /open_api/v3.0/uni_project/aweme/authorized/get/

**获取全域创编可用抖音号**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/uni_project/aweme/authorized/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854930373174281>（doc_id=1854930373174281）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| playlet_id | number(int64) | 是 | — | 专辑ID |
| page | number(int64) | 否 | — | 页码<br>默认值： 1 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.aweme_id_list | object[] | 抖音号列表 |
| data.aweme_id_list[].aweme_id | string | 抖音id |
| data.has_more | boolean | 是否已获取全部全域可用抖音号，允许值：<br>TRUE 仍存在未获取全域可用抖音号<br>FALSE 已获取 |
| request_id | string | 请求日志id |

### 备注

- `playlet_id`（专辑ID）必填；`page` 默认 1。
- 应答 `data.aweme_id_list[].aweme_id`；`has_more`（bool）：TRUE 仍存在未获取全域可用抖音号，FALSE 已获取全部。

## /open_api/v3.0/uni_project/monetization_mode/update/

**批量更新项目变现模式**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/uni_project/monetization_mode/update/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1858725855069700>（doc_id=1858725855069700）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 投放账户id |
| project_list | object[] | 是 | — | 项目id列表 |
| project_list[].project_id | number(int64) | 是 | — | 项目id |
| project_list[].monetization_mode | string | 是 | IAA 广告变现；IAA_AND_IAP 混合变现；IAP 用户付费 | 可选值:<br>IAA_AND_IAP 混合变现<br>IAP 用户付费 |
| project_list[].playlet_series_url | string | 否 | — | 短剧专辑链接<br>当monetization_mode= IAA_AND_IAP 混合变现时，有效且必填<br>仅支持传入IAA链接，如传入IAP链接，则更新失败<br>本次更新为全量更新 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.project_ids | number[] | 更新成功的项目ID集合 |
| data.errors | object[] | 更新失败的项目ID列表 |
| data.errors[].project_id | number(int64) | 项目ID |
| data.errors[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- `project_list[]` 每项含 project_id、monetization_mode（IAA_AND_IAP 混合变现 / IAP 用户付费）、playlet_series_url。
- `playlet_series_url`：当 monetization_mode=IAA_AND_IAP 时有效且必填；**仅支持传入 IAA 链接，如传入 IAP 链接则更新失败**；页面原文「本次更新为全量更新」。
- 应答 `data.project_ids`（number[]）更新成功的项目ID集合；`data.errors[]`{project_id, error_message} 更新失败的项目列表。
- **monetization_mode 差异**：页面正文可选值仅 IAA_AND_IAP / IAP；官方结构化 schema 的 enum 数组另含 `IAA 广告变现`。两处原文均照录。

## /open_api/v3.0/oc_project/material/get/

**获取投放项目下素材**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/oc_project/material/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854927239666825>（doc_id=1854927239666825）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| filtering | object | 是 | — | 过滤器 |
| filtering.material_type | string | 否 | IMAGE；VIDEO；CAROUSEL；TRIAL_PLAY_MATERIAL；INSTANT_PLAY_MATERIAL；OTHER；TITLE；MINI_PROGRAM_INFO | 素材类型，可选值，单次仅支持传入一个:<br>VIDEO 视频<br>TITLE 标题<br>IMAGE 图片<br>CAROUSEL 图文<br>TRIAL_PLAY_MATERIAL 试玩素材<br>INSTANT_PLAY_MATERIAL 直玩素材<br>MINI_PROGRAM_INFO 字节小程序&小游戏信息<br>OTHER 其他素材类型（包括产品信息、短剧专辑链接、落地页、行动号召等） |
| filtering.material_status_first | string | 否 | ALL 不限（包含已删除已终止）；ALL_EXCEPT_DELETE 不限；STATUS_DELETE 已删除；STATUS_DISABLE 未投放；STATUS_FROZEN 已终止；STATUS_OK 投放中；STATUS_TIME_DONE 已完成 | 素材一级状态过滤，可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| filtering.search_keyword | string | 否 | — | 搜索关键词，支持根据视频mid进行搜索，当且仅当material_type= VIDEO 视频、 IMAGE 图片、 CAROUSEL 图文时支持传入 |
| filtering.material_status_second | string | 否 | DOUYIN_ITEM_NOT_AVAILABLE_FOR_DELIVERY 抖音素材不可投放；ECOSYSTEM_RISK_FAILED 存在严重生态问题；GUIDE_VIDEO_NOT_EXIST 引导视频不存在；MATERIAL_STATUS_AUDIT 新建审核中；MATERIAL_STATUS_DISABLE 手动暂停；MATERIAL_STATUS_OFFLINE_AUDIT 审核不通过；MATERIAL_STATUS_PROCEDURAL_DISABLE 系统暂停 | 素材二级状态过滤，可选值：<br>MATERIAL_STATUS_OFFLINE_AUDIT 审核不通过<br>MATERIAL_STATUS_AUDIT 新建审核中<br>MATERIAL_STATUS_DISABLE 手动暂停<br>MATERIAL_STATUS_PROCEDURAL_DISABLE 系统暂停<br>DOUYIN_ITEM_NOT_AVAILABLE_FOR_DELIVERY 关联抖音视频不可投<br>GUIDE_VIDEO_NOT_EXIST 引导视频不存在 |
| filtering.material_ids | string[] | 否 | — | 素材ID精确过滤（MID），仅支持五类主素材<br>VIDEO 视频<br>IMAGE 图片<br>CAROUSEL 图文<br>TRIAL_PLAY_MATERIAL 试玩素材<br>INSTANT_PLAY_MATERIAL 直玩素材<br>单次传入500个素材 |
| filtering.material_create_start_time | string(datetime) | 否 | — | 素材添加项目的起始时间，格式yyyy-mm-dd hh:mm:ss。必须与结束时间material_create_end_time结合使用 |
| filtering.material_create_end_time | string(datetime) | 否 | — | 素材添加项目的结束时间，格式yyyy-mm-dd hh:mm:ss。时间跨度不能超过60天 |
| page | number(int64) | 否 | — | 页码，默认值：1，当且仅当material_type= VIDEO 视频、 IMAGE 图片、 CAROUSEL 图文、 TRIAL_PLAY_MATERIAL 试玩素材时支持传入 |
| page_size | number(int64) | 否 | — | 页面大小，允许值：10, 20, 50, 100，默认值：10，仅material_type= VIDEO 视频时支持传入 |
| cursor | number(int64) | 否 | — | 页码游标值。首次拉取传 0；与 page 同时传入时 cursor 优先。 |
| count | number(int64) | 否 | — | 游标分页每页数据量，取值范围 1-100；cursor 已传且 count 缺省时按 10 处理。 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.video_material_list | object[] | 视频素材信息 |
| data.video_material_list[].video_id | string | 视频id |
| data.video_material_list[].video_cover_uri | string | 视频封面图片ID ，字段即将下线，请及时切换video_cover_id。 |
| data.video_material_list[].material_id | number(int64) | 素材id |
| data.video_material_list[].material_opt_status | string | 视频素材操作状态，枚举值：<br>DISABLE 暂停<br>ENABLE 启用 |
| data.video_material_list[].material_status_frist | string | 可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| data.video_material_list[].material_status_second | string[] | 可选值:<br>MATERIAL_STATUS_AUDIT 未投放-新建审核中<br>MATERIAL_STATUS_AWEME_ACCOUNT_OFFLINE 抖音账号不可投放<br>MATERIAL_STATUS_DELETE 已删除-素材已删除<br>MATERIAL_STATUS_DISABLE 已暂停<br>MATERIAL_STATUS_NO_SCHEDULE 不在投放时段<br>MATERIAL_STATUS_OFFLINE_AUDIT 未投放-审核不通过<br>MATERIAL_STATUS_OFFLINE_BALANCE 账户余额不足<br>MATERIAL_STATUS_OFFLINE_BUDGET 账户超出预算<br>MATERIAL_STATUS_PRODUCT_OFFLINE 商品不可投放<br>MATERIAL_STATUS_PROJECT_DISABLE 项目已暂停<br>MATERIAL_STATUS_PROJECT_OFFLINE_BUDGET 项目超出预算<br>MATERIAL_STATUS_TIME_NO_REACH 未达投放时间<br>PROJECT_STATUS_DELETE 已删除-项目已删除<br>PROJECT_STATUS_FROZEN 已终止-项目已终止<br>PROJECT_STATUS_REAUDIT 未投放-修改审核中 |
| data.video_material_list[].image_mode | string | 可选值:<br>CREATIVE_IMAGE_MODE_CALIBRATION_FREE_VIDEO 穿山甲免校验<br>CREATIVE_IMAGE_MODE_UNION_SPLASH_VIDEO 互动创意<br>CREATIVE_IMAGE_MODE_VIDEO 视频<br>CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 抖音&火山竖版视频素材 |
| data.video_material_list[].item_id | number(int64) | 抖音主页视频id |
| data.video_material_list[].video_cover_id | string | 视频封面图片ID<br>（视频封面与视频绑定能力全新上线， 素材库视频与创编主流程使用的同一视频素材将对应绑定同一个视频封面 。） |
| data.video_material_list[].guide_video_id | string | 引导视频id |
| data.video_material_list[].material_create_time | string | 素材添加到项目的时间 |
| data.image_material_list | object[] | 图片素材信息 |
| data.image_material_list[].image_mode | string | 素材类型，可选值:<br>CREATIVE_IMAGE_MODE_LARGE<br>CREATIVE_IMAGE_MODE_LARGE_VERTICAL 大图竖版图<br>CREATIVE_IMAGE_MODE_SMALL<br>SEARCH_AD_SMALL_IMAGE 搜索小图<br>TOUTIAO_SEARCH_AD_IMAGE 搜索大图 |
| data.image_material_list[].image_ids | string[] | 图片id |
| data.image_material_list[].material_id | number(int64) | 素材id |
| data.image_material_list[].material_status_frist | string | 素材一级状态，可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| data.image_material_list[].material_status_second | string[] | 素材二级状态，可选值：<br>PROJECT_STATUS_DELETE 项目已删除<br>MATERIAL_STATUS_DELETE 素材已删除<br>PROJECT_STATUS_FROZEN 项目已终止<br>MATERIAL_STATUS_FROZEN 素材已终止<br>MATERIAL_STATUS_OFFLINE_AUDIT 审核不通过<br>MATERIAL_STATUS_AUDIT 新建审核中<br>MATERIAL_STATUS_DISABLE 手动暂停<br>MATERIAL_STATUS_PROCEDURAL_DISABLE 系统暂停<br>DOUYIN_ITEM_NOT_AVAILABLE_FOR_DELIVERY 关联抖音视频不可投<br>GUIDE_VIDEO_NOT_EXIST 引导视频不存在<br>MATERIAL_STATUS_PROJECT_DISABLE 项目已暂停<br>LACK_BASIC_MATERIAL_STATUS项目素材待补充<br>PROJECT_STATUS_AUDIT 项目新建审核中<br>PROJECT_STATUS_REAUDIT 项目修改审核中<br>PROJECT_AUDIT_REJECTED 项目审核不通过<br>MATERIAL_STATUS_OFFLINE_BALANCE 账户余额不足<br>MATERIAL_STATUS_OFFLINE_BUDGET 账户超出预算<br>SHARED_WALLET_EXCEEDS_BUDGET 共享钱包超出预算<br>MATERIAL_STATUS_PROJECT_OFFLINE_BUDGET 项目超出预算<br>MATERIAL_STATUS_TIME_NO_REACH 项目未达投放时间<br>PROJECT_REACHED_DELIVERY_DURATION 项目已达投放时长<br>MATERIAL_STATUS_NO_SCHEDULE 项目不在投放时段<br>LIVE_ROOM_NOT_AVAILABLE_FOR_DELIVERY 关联直播间不可投<br>MATERIAL_STATUS_PRODUCT_OFFLINE 商品不可投放<br>APP_NOT_AVAILABLE_FOR_DELIVERY 应用不可投<br>ACCOUNT_AVATAR_NOT_AVAILABLE_FOR_DELIVERY 账户头像不可投<br>MATERIAL_STATUS_AWEME_ACCOUNT_OFFLINE 抖音账号不可投放<br>ECOSYSTEM_ RISK_ FAILED 存在严重生态问题 |
| data.image_material_list[].material_create_time | string | 素材添加到项目的时间 |
| data.carousel_material_list | object[] | 图文素材信息 |
| data.carousel_material_list[].carousel_id | number(int64) | 图文id |
| data.carousel_material_list[].item_id | number(int64) | 抖音主页图文id |
| data.carousel_material_list[].video_hp_visibility | string | 可选值:<br>ALWAYS_VISIBLE 主页始终可见<br>HIDE_VIDEO_ON_HP 主页隐藏<br>默认值: HIDE_VIDEO_ON_HP |
| data.carousel_material_list[].material_status_frist | string | 可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| data.carousel_material_list[].material_status_second | string[] | 可选值:<br>ACCOUNT_AVATAR_NOT_AVAILABLE_FOR_DELIVERY 账户头像不可投放<br>ACCOUNT_EXCEEDS_BUDGET 账户超出预算<br>ALL_KEYWORDS_DELETED 关键词均删除<br>ALL_MATERIALS_DELETED 素材均删除<br>APP_NOT_AVAILABLE_FOR_DELIVERY 应用不可投放<br>AUDIT_REJECTED 审核不通过<br>DOUYIN_ACCOUNT_NOT_AVAILABLE_FOR_DELIVERY 抖音账户不可投放<br>GOODS_NOT_AVAILABLE_FOR_DELIVERY 商品不可投放<br>LIVE_ROOM_NOT_AVAILABLE_FOR_DELIVERY 直播间不可投放<br>LIVE_ROOM_NOT_STARTED 直播间未开播<br>MANUAL_PAUSE 手动暂停<br>MODIFY_AUDITING 修改审核中<br>NEW_CREATED_AUDITING 新建审核中<br>PAUSED 已暂停<br>PROJECT_AUDIENCE_SETTING_CONFLICT 与项目定向设置冲突<br>PROJECT_AUDIT_REJECTED 项目审核不通过<br>PROJECT_BALANCE_INSUFFICIENT 项目余额不足<br>PROJECT_COMPLETED 项目已完成<br>PROJECT_DELETED 项目已删除<br>PROJECT_EXCEEDS_BUDGET 项目超出预算<br>PROJECT_MODIFY_AUDITING 项目修改审核中<br>PROJECT_NEW_CREATED_AUDITING 项目新建审核中<br>PROJECT_NOT_IN_DELIVERY_PERIOD 项目不在投放时段<br>PROJECT_NOT_REACH_DELIVERY_TIME 项目未达投放时间<br>PROJECT_PAUSED 项目已暂停<br>PROJECT_REACHED_DELIVERY_DURATION 项目已达投放时长<br>PROJECT_TERMINATED 项目已终止<br>REACHED_REGULATION_BUDGET_UPPER_LIMIT 到达调控预算上限<br>SHARED_WALLET_EXCEEDS_BUDGET 共享钱包超出预算<br>TASK_BID_EXCEEDS_PROJECT_BUDGET 不满足出价或预算规则<br>TASK_DELETED 任务已删除<br>TASK_REACHED_REGULATION_DURATION 任务已达调控时长<br>TASK_TERMINATED 任务已终止 |
| data.carousel_material_list[].description | string | 图文作品描述 |
| data.carousel_material_list[].material_create_time | string | 素材添加到项目的时间 |
| data.trial_play_material_list | object[] | 试玩素材信息 |
| data.trial_play_material_list[].app_play_uri | string | 试玩素材uri |
| data.trial_play_material_list[].guide_video_id | string | 引导视频ID |
| data.trial_play_material_list[].image_mode | string | 可选值:<br>CREATIVE_IMAGE_MODE_TRIAL_PLAY_LARGE 试玩横屏<br>CREATIVE_IMAGE_MODE_TRIAL_PLAY_VERTICAL 试玩竖屏 |
| data.trial_play_material_list[].app_play_version | string | 试玩素材版本号 |
| data.trial_play_material_list[].file_name | string | 素材名称 |
| data.trial_play_material_list[].material_id | number(int64) | 素材id |
| data.trial_play_material_list[].material_status_frist | string | 可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| data.trial_play_material_list[].material_status_second | string[] | 可选值:<br>ACCOUNT_AVATAR_NOT_AVAILABLE_FOR_DELIVERY 账户头像不可投放<br>APP_NOT_AVAILABLE_FOR_DELIVERY 应用不可投放<br>DOUYIN_ITEM_NOT_AVAILABLE_FOR_DELIVERY 抖音素材不可投放<br>GUIDE_VIDEO_NOT_EXIST 引导视频不存在<br>LACK_BASIC_MATERIAL_STATUS 项目素材待补充<br>LIVE_ROOM_NOT_AVAILABLE_FOR_DELIVERY 直播间不可投放<br>MATERIAL_STATUS_AUDIT 新建审核中<br>MATERIAL_STATUS_AWEME_ACCOUNT_OFFLINE 抖音账号不可投放<br>MATERIAL_STATUS_DELETE 素材已删除<br>MATERIAL_STATUS_DISABLE 手动暂停<br>MATERIAL_STATUS_FROZEN 素材已终止<br>MATERIAL_STATUS_NO_SCHEDULE 项目不在投放时段<br>MATERIAL_STATUS_OFFLINE_AUDIT 审核不通过<br>MATERIAL_STATUS_OFFLINE_BALANCE 账户余额不足<br>MATERIAL_STATUS_OFFLINE_BUDGET 账户超出预算<br>MATERIAL_STATUS_PROCEDURAL_DISABLE 系统暂停<br>MATERIAL_STATUS_PRODUCT_OFFLINE 商品不可投放<br>MATERIAL_STATUS_PROJECT_DISABLE 项目已暂停<br>MATERIAL_STATUS_PROJECT_OFFLINE_BUDGET 项目超出预算<br>MATERIAL_STATUS_TIME_NO_REACH 项目未达投放时间<br>PROJECT_AUDIT_REJECTED 项目审核不通过<br>PROJECT_REACHED_DELIVERY_DURATION 项目已达投放时长<br>PROJECT_STATUS_AUDIT 项目新建审核中<br>PROJECT_STATUS_DELETE 项目已删除<br>PROJECT_STATUS_FROZEN 项目已终止<br>PROJECT_STATUS_REAUDIT 项目修改审核中<br>SHARED_WALLET_EXCEEDS_BUDGET 共享钱包超出预算 |
| data.trial_play_material_list[].material_create_time | string | 素材添加到项目的时间 |
| data.instant_play_material_list | object[] | 直玩素材信息 |
| data.instant_play_material_list[].app_play_uri | string | 直玩素材uri |
| data.instant_play_material_list[].image_mode | string | 直玩素材类型，可选值:<br>CREATIVE_IMAGE_MODE_INSTANT_PLAY 直玩 |
| data.instant_play_material_list[].cover_uri | string | 封面图 |
| data.instant_play_material_list[].file_name | string | 封面名称 |
| data.instant_play_material_list[].material_id | number(int64) | 素材id |
| data.instant_play_material_list[].material_status_frist | string | 可选值:<br>STATUS_DELETE 已删除<br>STATUS_DISABLE 未投放<br>STATUS_FROZEN 已终止<br>STATUS_OK 投放中<br>STATUS_TIME_DONE 已完成 |
| data.instant_play_material_list[].material_status_second | string[] | 可选值:<br>ACCOUNT_AVATAR_NOT_AVAILABLE_FOR_DELIVERY 账户头像不可投放<br>APP_NOT_AVAILABLE_FOR_DELIVERY 应用不可投放<br>DOUYIN_ITEM_NOT_AVAILABLE_FOR_DELIVERY 抖音素材不可投放<br>GUIDE_VIDEO_NOT_EXIST 引导视频不存在<br>LACK_BASIC_MATERIAL_STATUS 项目素材待补充<br>LIVE_ROOM_NOT_AVAILABLE_FOR_DELIVERY 直播间不可投放<br>MATERIAL_STATUS_AUDIT 新建审核中<br>MATERIAL_STATUS_AWEME_ACCOUNT_OFFLINE 抖音账号不可投放<br>MATERIAL_STATUS_DELETE 素材已删除<br>MATERIAL_STATUS_DISABLE 手动暂停<br>MATERIAL_STATUS_FROZEN 素材已终止<br>MATERIAL_STATUS_NO_SCHEDULE 项目不在投放时段<br>MATERIAL_STATUS_OFFLINE_AUDIT 审核不通过<br>MATERIAL_STATUS_OFFLINE_BALANCE 账户余额不足<br>MATERIAL_STATUS_OFFLINE_BUDGET 账户超出预算<br>MATERIAL_STATUS_PROCEDURAL_DISABLE 系统暂停<br>MATERIAL_STATUS_PRODUCT_OFFLINE 商品不可投放<br>MATERIAL_STATUS_PROJECT_DISABLE 项目已暂停<br>MATERIAL_STATUS_PROJECT_OFFLINE_BUDGET 项目超出预算<br>MATERIAL_STATUS_TIME_NO_REACH 项目未达投放时间<br>PROJECT_AUDIT_REJECTED 项目审核不通过<br>PROJECT_REACHED_DELIVERY_DURATION 项目已达投放时长<br>PROJECT_STATUS_AUDIT 项目新建审核中<br>PROJECT_STATUS_DELETE 项目已删除<br>PROJECT_STATUS_FROZEN 项目已终止<br>PROJECT_STATUS_REAUDIT 项目修改审核中<br>SHARED_WALLET_EXCEEDS_BUDGET 共享钱包超出预算 |
| data.instant_play_material_list[].material_create_time | string | 素材添加到项目的时间 |
| data.title_material_list | object[] | 标题素材信息 |
| data.title_material_list[].title | string | 创意标题 |
| data.title_material_list[].material_id | number(int64) | 创意id |
| data.title_material_list[].dpa_word_list | number[] | DPA词包ID列表 |
| data.title_material_list[].word_list | number[] | 标题素材词包ID列表 |
| data.product_info | object | 产品信息 |
| data.product_info.titles | string[] | 产品名称 |
| data.product_info.image_ids | string[] | 产品主图 |
| data.product_info.selling_points | string[] | 产品卖点 |
| data.product_info.product_name_type | string | 产品名称类型 可选值:<br>CUSTOM 自定义字段<br>DPA 产品库字段 |
| data.product_info.product_image_type | string | 产品主图类型， 可选值:<br>CUSTOM 自定义字段<br>DPA 产品库字段 |
| data.product_info.product_selling_point_type | string | 产品卖点类型 可选值:<br>CUSTOM 自定义字段<br>DPA 产品库字段 |
| data.product_info.product_name_fields | string[] | DPA产品库名称字段 |
| data.product_info.product_image_fields | string[] | DPA产品库图片字段 |
| data.product_info.product_selling_point_fields | string[] | DPA产品库卖点类型字段 |
| data.call_to_action_buttons | string[] | 行动号召 |
| data.playlet_series_url_info | object[] | 短剧专辑链接信息 |
| data.playlet_series_url_info[].playlet_series_url_type | string | 短剧专辑类型，可选值:<br>IAA<br>IAP |
| data.playlet_series_url_info[].playlet_series_url | string | 短剧专辑链接 |
| data.external_url_material_list | string[] | 普通落地页链接素材 |
| data.mini_program_info | object | 字节小程序&小游戏信息 |
| data.mini_program_info.url | string | 字节小程序&小游戏调起链接 |
| data.mini_program_info.app_id | string | 小程序/小游戏原始id |
| data.mini_program_info.start_path | string | 启动路径 |
| data.mini_program_info.params | string | 页面检测参数 |
| data.playlet_series_url_list | string[] | 短剧合集链接url |
| data.params_type | string | 落地页链接类型<br>- DPA 商品库所含链接，可选值：<br>CUSTOM 自定义链接<br>DPA 商品库所含链接 |
| data.external_url_field | string | 落地页链接字段选择 |
| data.external_url_params | string | 落地页检测参数 |
| data.web_url_material_list | string[] | 应用下载详情页 |
| data.component_material_list | object[] | 创意组件信息。数量上限为2 |
| data.component_material_list[].component_id | number(int64) | 组件id |
| data.anchor_material_list | object[] | 原生锚点素材 |
| data.anchor_material_list[].anchor_id | string | 原生锚点id |
| data.anchor_material_list[].anchor_type | string | 可选值:<br>APP_GAME 应用下载-游戏锚点<br>APP_INTERNET_SERVICE 应用下载-网服锚点<br>APP_SHOP 应用下载-电商锚点<br>INSURANCE 保险锚点<br>ONLINE_SUBSCRIBE 通用锚点<br>PRIVATE_CHAT 私信锚点<br>SHOPPING_CART 购物车锚点 |
| data.anchor_related_type | string | 可选值:<br>AUTO 自动生成<br>OFF 不启用<br>SELECT 手动选择 |
| data.open_url_type | string | 直达链接类型，可选值:<br>CUSTOM 自定义链接<br>DPA 商品库所含链接<br>NONE 不启用 |
| data.open_url_params | string | 直达链接检测参数 |
| data.open_url_field | string | 直达链接字段选择 |
| data.open_url | string | 直达链接 |
| data.open_urls | string[] | 多直达链接 |
| data.ulink_url_type | string | 直达链接备用链接类型，可选值:<br>UNIVERSAL_LINK<br>APPLINK |
| data.ulink_url | string | 直达链接备用链接 |
| data.page_info | object | 分页信息 |
| data.page_info.page | number(int64) | 页码 |
| data.page_info.page_size | number(int64) | 页面大小 |
| data.page_info.total_number | number(int64) | 总数 |
| data.page_info.total_page | number(int64) | 总页数 |
| data.cursor_info | object | 游标分页信息，仅实际采用 cursor 分页时返回。 |
| data.cursor_info.has_more | boolean | 是否还有下一页。 |
| data.cursor_info.count | number(int64) | 本次游标分页请求的数据量。 |
| data.cursor_info.cursor | number(int64) | 下一次分页拉取使用的游标值。 |
| request_id | string | 请求日志id |

### 备注

- **path 前缀为 `oc_project`**：`/open_api/v3.0/oc_project/material/get/`。
- `filtering` 必填；`filtering.material_type` 单次仅支持传入一个：VIDEO / TITLE / IMAGE / CAROUSEL / TRIAL_PLAY_MATERIAL / INSTANT_PLAY_MATERIAL / MINI_PROGRAM_INFO / OTHER（其他素材类型，包括产品信息、短剧专辑链接、落地页、行动号召等）。
- `search_keyword` 支持按视频 mid 搜索，仅 material_type=VIDEO/IMAGE/CAROUSEL 时支持；`material_ids` 精确过滤（MID）仅支持五类主素材（VIDEO/IMAGE/CAROUSEL/TRIAL_PLAY_MATERIAL/INSTANT_PLAY_MATERIAL），单次传入 500 个。
- 素材添加时间过滤必须成对使用，时间跨度不能超过 60 天。
- `page` 仅 material_type=VIDEO/IMAGE/CAROUSEL/TRIAL_PLAY_MATERIAL 时支持，默认 1；`page_size` 仅 material_type=VIDEO 时支持，允许值 10,20,50,100，默认 10。
- **游标分页**：`cursor` 首次拉取传 0，与 page 同时传入时 cursor 优先；`count` 取值 1-100，cursor 已传且 count 缺省时按 10 处理；应答 `cursor_info`{has_more, count, cursor} 仅实际采用 cursor 分页时返回。
- **应答字段名原文为 `material_status_frist`（frist 拼写，非 first），各素材列表均如此，照录。**
- `video_material_list[].video_cover_uri` 字段即将下线，请及时切换 video_cover_id。
- `component_material_list` 数量上限为 2。
- 应答含 video_material_list / image_material_list / carousel_material_list / trial_play_material_list / title_material_list / product_info / call_to_action_buttons / playlet_series_url_info / external_url_material_list / web_url_material_list / component_material_list / anchor_material_list / instant_play_material_list / mini_program_info 等，按 material_type 返回对应块。

## /open_api/v3.0/oc_project/material/create/

**批量添加投放项目下素材**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/oc_project/material/create/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854929666566346>（doc_id=1854929666566346）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| video_material_list | object[] | 否 | — | 视频素材信息，单次追投最多可添加200个素材（总素材量，包含视频、图片、图文） |
| video_material_list[].image_mode | string | 否 | CREATIVE_IMAGE_MODE_VIDEO 视频；CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 抖音&火山竖版视频素材 | 视频素材类型，允许值：<br>CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 竖版视频<br>CREATIVE_IMAGE_MODE_VIDE 横版视频 |
| video_material_list[].video_id | string | 否 | — | 视频ID，可通过 【获取视频素材】 或【获取账户可用的组织视频列表】接口获得 |
| video_material_list[].video_cover_id | string | 否 | — | 视频封面图片ID，可通过 【获取视频素材】 【获取图片素材】 【获取视频智能封面】 接口获取。<br>（视频封面与视频绑定能力全新上线， 素材库视频与创编主流程使用的同一视频素材将对应绑定同一个视频封面，即 您无需填写该字段， 传入即为人工自提- 更改封面图片绑定关系。） |
| video_material_list[].item_id | number(int64) | 否 | — | 注：当aweme_id传入时有效<br>抖音短视频ID，通过 【获取抖音主页视频】 或 【获取抖音授权关系】 获取主页视频或合作授权视频 |
| video_material_list[].video_hp_visibility | string | 否 | ALWAYS_VISIBLE 主页始终可见；HIDE_VIDEO_ON_HP 主页隐藏 | 视频素材主页可见性设置，该参数只针对非抖音视频生效，可选值:<br>ALWAYS_VISIBLE 主页始终可见<br>HIDE_VIDEO_ON_HP 仅单次展示可见（默认值） |
| video_material_list[].guide_video_id | string | 否 | — | 引导视频id |
| image_material_list | object[] | 否 | — | 图片素材信息，，单次追投最多可添加200个素材 |
| image_material_list[].image_mode | string | 否 | AWEME_PHOTO 图文素材；CREATIVE_IMAGE_MODE_AWEME_LIVE 直播创意素材；CREATIVE_IMAGE_MODE_CALIBRATION_FREE_SINGLE_IMAGE 穿山甲免校验；CREATIVE_IMAGE_MODE_CALIBRATION_FREE_VIDEO 穿山甲免校验；CREATIVE_IMAGE_MODE_DISPLAY_WINDOW 头条搜索广告通用橱窗样式；CREATIVE_IMAGE_MODE_GIF；CREATIVE_IMAGE_MODE_GROUP；CREATIVE_IMAGE_MODE_INSTANT_PLAY 直玩；CREATIVE_IMAGE_MODE_LARGE；CREATIVE_IMAGE_MODE_LARGE_VERTICAL 大图竖版图；CREATIVE_IMAGE_MODE_PLAYABLE_HORIZONTAL 试玩；CREATIVE_IMAGE_MODE_PLAYABLE_VERTICAL 试玩；CREATIVE_IMAGE_MODE_SMALL；CREATIVE_IMAGE_MODE_TRIAL_PLAY_LARGE 试玩横屏；CREATIVE_IMAGE_MODE_TRIAL_PLAY_VERTICAL 试玩竖屏；CREATIVE_IMAGE_MODE_UNION_SPLASH 网盟开屏；CREATIVE_IMAGE_MODE_UNION_SPLASH_VIDEO 互动创意；CREATIVE_IMAGE_MODE_VIDEO 视频；CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 抖音&火山竖版视频素材；SEARCH_AD_SMALL_IMAGE 搜索广告小图；TOUTIAO_SEARCH_AD_IMAGE 搜索大图 | 可选值:<br>AWEME_PHOTO 图文素材<br>CREATIVE_IMAGE_MODE_AWEME_LIVE 直播创意素材<br>CREATIVE_IMAGE_MODE_CALIBRATION_FREE_SINGLE_IMAGE 穿山甲免校验<br>CREATIVE_IMAGE_MODE_CALIBRATION_FREE_VIDEO 穿山甲免校验<br>CREATIVE_IMAGE_MODE_DISPLAY_WINDOW 头条搜索投放通用橱窗样式<br>CREATIVE_IMAGE_MODE_GIF<br>CREATIVE_IMAGE_MODE_GROUP<br>CREATIVE_IMAGE_MODE_INSTANT_PLAY 直玩<br>CREATIVE_IMAGE_MODE_LARGE<br>CREATIVE_IMAGE_MODE_LARGE_VERTICAL 大图竖版图<br>CREATIVE_IMAGE_MODE_PLAYABLE_HORIZONTAL 试玩<br>CREATIVE_IMAGE_MODE_PLAYABLE_VERTICAL 试玩<br>CREATIVE_IMAGE_MODE_SMALL<br>CREATIVE_IMAGE_MODE_TRIAL_PLAY_LARGE 试玩横屏<br>CREATIVE_IMAGE_MODE_TRIAL_PLAY_VERTICAL 试玩竖屏<br>CREATIVE_IMAGE_MODE_UNION_SPLASH 网盟开屏<br>CREATIVE_IMAGE_MODE_UNION_SPLASH_VIDEO 互动创意<br>CREATIVE_IMAGE_MODE_VIDEO 视频<br>CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 抖音&火山竖版视频素材<br>SEARCH_AD_SMALL_IMAGE 搜索小图<br>TOUTIAO_SEARCH_AD_IMAGE 搜索大图 |
| image_material_list[].images | string | 否 | — | 图片ID |
| image_material_list[].image_id | string | 否 | — | 图片ID，可通过 【获取图片素材】 接口获得 |
| carousel_material_list | object[] | 否 | — | 图文素材信息 |
| carousel_material_list[].carousel_id | string | 否 | — | 图文id，可通过【 获取图文素材 】接口获得 |
| carousel_material_list[].item_id | string | 否 | — | 抖音图文id，需从【 获取创编可用的抖音图文素材 】接口获取item_id传入<br>注意：投放抖音图文素材时，只需要传入item_id，不要传入carousel_id（2个同时传入只会使用item_id） |
| carousel_material_list[].video_hp_visibility | string | 否 | ALWAYS_VISIBLE 主页始终可见；HIDE_VIDEO_ON_HP 主页隐藏 | 抖音主页图文可见性，可选值:<br>ALWAYS_VISIBLE 主页始终可见<br>HIDE_VIDEO_ON_HP 主页隐藏 |
| trial_play_material_list | object[] | 否 | — | 试玩素材信息 |
| trial_play_material_list[].app_play_uri | string | 否 | — | 试玩/直玩素材uri ，可通过【 获取试玩/直玩素材列表 】获取可用于创编的素材 |
| trial_play_material_list[].guide_video_id | string | 否 | — | 通过【 上传视频 】接口上传引导视频，获取引导视频的video_id<br>引导视频ID，引导视频时长≥5s，文件≤100M |
| title_material_list | object[] | 否 | — | 标题信息 |
| title_material_list[].title | string | 否 | — | 标题 |
| title_material_list[].dpa_word_list | number[] | 否 | — | 创建DPA单元时可以选择传入DPA词包ID列表，动态词包与DPA词包总数最多为2 |
| title_material_list[].word_list | number[] | 否 | — | 词包ID列表 |
| instant_play_material_list | object[] | 否 | — | 直玩素材信息 |
| instant_play_material_list[].app_play_uri | string | 否 | — | 试玩/直玩素材uri ，可通过【 获取试玩/直玩素材列表 】获取可用于创编的素材 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.error_list | object[] | 部分成功时的逐项失败明细；全部成功或下游整体失败时为空 |
| data.error_list[].object_type | string | 错误对象类型 |
| data.error_list[].error_code | string | 错误码 |
| data.error_list[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- **path 前缀为 `oc_project`**：`/open_api/v3.0/oc_project/material/create/`。
- **200 素材上限（原文）**：video_material_list 描述「视频素材信息，单次追投最多可添加200个素材（总素材量，包含视频、图片、图文）」；image_material_list 描述「图片素材信息，，单次追投最多可添加200个素材」。即 200 为单次追投的总素材量上限（视频+图片+图文合计），写在字段描述中，无独立上限字段。
- **拼写差异**：video_material_list[].image_mode 的页面正文/描述原文横版视频枚举为 `CREATIVE_IMAGE_MODE_VIDE`（缺一个 O），而官方结构化 schema 的 enum 数组为 `CREATIVE_IMAGE_MODE_VIDEO`。两种原文均照录，对接时以实测为准。
- `carousel_material_list`：投放抖音图文素材时只需传入 item_id，不要传入 carousel_id（2 个同时传入只会使用 item_id）；item_id 需从【获取创编可用的抖音图文素材】接口获取。
- `trial_play_material_list[].guide_video_id`：引导视频时长≥5s，文件≤100M。
- `dpa_word_list`：创建DPA单元时可选传入DPA词包ID列表，动态词包与DPA词包总数最多为 2。
- **应答 error_list 语义（原文）**：「部分成功时的逐项失败明细；全部成功或下游整体失败时为空」。

## /open_api/v3.0/oc_project/material/delete/

**删除投放项目下素材**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/oc_project/material/delete/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854929995241483>（doc_id=1854929995241483）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| material_id | number[] | 是 | — | 素材id |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| request_id | string | 请求日志id |

### 备注

- **path 前缀为 `oc_project`**：`/open_api/v3.0/oc_project/material/delete/`。
- 页面原文限制：仅支持删除「巨量营销」账户下的素材；不支持删除项目下「已删除」的素材（传入的 material_id 在全域项目下状态为已删除时，调用报错「全域项目下不存在素材」）；**不支持将全域项目下素材全部删除**——当项目下仅剩 1 个素材时再调用删除会报错。
- `material_id` 为 number[]（批量）。应答 data 无业务字段（仅通用封套）。

## /open_api/v3.0/oc_project/material/status/update/

**批量修改投放项目下素材状态**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/oc_project/material/status/update/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854930119140363>（doc_id=1854930119140363）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| project_id | number(int64) | 是 | — | 项目id |
| data | object[] | 是 | — | 批量更新项目素材启用状态，list长度限制1～100 |
| data[].material_id | number(int64) | 是 | — | 素材id |
| data[].opt_status | string | 是 | DISABLE 暂停；ENABLE 启用 | 可选值:<br>DISABLE 暂停<br>ENABLE 启用 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.project_id | number(int64) | 项目id |
| data.material_ids | number[] | 更新成功的素材ID集合 |
| data.errors | object[] | 更新失败的素材列表 |
| data.errors[].material_id | number(int64) | 素材ID |
| data.errors[].message | string | 失败原因 |
| request_id | string | 请求日志id |

### 备注

- **path 前缀为 `oc_project`**：`/open_api/v3.0/oc_project/material/status/update/`。
- 请求 `data[]` 长度限制 1~100；每项 material_id（必填）+ opt_status（必填，DISABLE 暂停 / ENABLE 启用）。
- 应答 `data.project_id`、`data.material_ids`（number[] 更新成功的素材ID集合）、`data.errors[]`{material_id, message 失败原因}。

## /open_api/2/tools/aweme_auth_list/

**获取抖音授权关系**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/2/tools/aweme_auth_list/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1729983667746823>（doc_id=1729983667746823）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户ID |
| filtering | object | 否 | — | 筛选条件，多个查询条件之间是 「与」 的关系 |
| filtering.auth_scenario | string | 否 | LIVE；VIDEO | 全域授权类型 可选值:<br>LIVE 全域-推直播<br>VIDEO 全域-推商品 |
| filtering.auth_type | string[] | 否 | — | 授权类型，允许值：<br>AWEME_ACCOUNT 抖音号授权<br>LIVE_ACCOUNT 直播授权<br>VIDEO_ITEM 单视频授权<br>AWEME_HOMEPAGE 主页作品授权<br>说明： 创建营销时，如果选用的aweme_id（抖音号）为「抖音号授权」，此时可以在营销下添加新的视频素材并发布到所选抖音号下进行投放；而当所选抖音号的授权类型为「主页作品授权」时，不具备此能力<br>抖音号授权：授权使用抖音号发布作品并投放、使用主页全部视频/图文投放、直播间引流<br>主页作品授权：授权使用抖音号主页全部视频/图文投放、直播间引流 |
| filtering.auth_status | string[] | 否 | — | 授权状态，允许值：<br>AUTHRIZED 授权中<br>AUTHRIZING 待授权确认<br>INVALID 授权失效 |
| filtering.aweme_ids | string[] | 否 | — | 按抖音号id过滤结果，最长传入50个 |
| filtering.item_ids | number[] | 否 | — | 按抖音视频id过滤结果，一次最多允许查询50个 |
| filtering.warning_types | string[] | 否 | — | 抖音授权关系警告信息类型，您可使用此筛选查询所有可能存在问题的授权关系。支持枚举，一条授权关系可能存在 多种 警告类型：<br>UNQUALIFIED 不达要求，表示发起授权的抖音号未达到要求，详细未达门槛信息可通过应答参数中的auth_threshold_info获取<br>UNQUALIFIED_PENDING_EXPIRE 不达要求-即将解除，表示平台定期巡检并解除不符合授权要求的【个人抖音号授权关系】 您可按需将您的个人抖音号升级为抖音企业机构账号或督促账号满足要求<br>仅当auth_type= AWEME_ACCOUNT 抖音号授权时，可能会有此警告<br>AUTHOR_REUQUEST_PENDING_EXPIRE 申请解除中，表示抖音号作者已发起解除授权申请，您需要及时联系作者或同意解除 |
| filtering.dpa_id | number(int64) | 否 | — | DPA商品库id |
| page | number(int64) | 否 | — | 页码 |
| page_size | number(int64) | 否 | — | 页面大小 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.list | object[] | 数据列表 |
| data.list[].auth_scenarios | string[] | 全域授权类型 可选值:<br>LIVE 全域-推直播<br>VIDEO 全域-推商品 |
| data.list[].auth_type | string | 授权类型，枚举值：<br>AWEME_ACCOUNT 抖音号授权<br>LIVE_ACCOUNT 直播授权<br>VIDEO_ITEM 单个作品授权<br>AWEME_HOMEPAGE 主页作品授权<br>说明： 创建营销时，如果选用的aweme_id（抖音号）为「抖音号授权」，此时可以在营销下添加新的视频素材并发布到所选抖音号下进行投放；而当所选抖音号的授权类型为「主页作品授权」时，不具备此能力<br>抖音号授权：授权使用抖音号发布作品并投放、使用主页全部视频/图文投放、直播间引流<br>主页作品授权：授权使用抖音号主页全部视频/图文投放、直播间引流 |
| data.list[].aweme_id | string | 抖音号id |
| data.list[].aweme_name | string | 抖音账号名称 |
| data.list[].aweme_user_type | string | 可选值:<br>ENTERPRISE 企业机构号<br>SINGLE 个人号 |
| data.list[].auth_status | string | 授权状态 可选值:<br>AUTHRIZED 授权中<br>AUTHRIZING 待授权确认<br>INVALID 授权失效 |
| data.list[].sub_status | string | 授权子状态 可选值:<br>AWEME_REVOKE_REQUEST 创作者发起解除申请<br>CONFIRM_REVOKE_REQUEST 同意解除授权申请<br>ENTERPRISE_AUTH_RELEASE 企业号身份撤销，不满足授权要求<br>INVALID_CANCEL 主动操作解除授权<br>INVALID_EXPIRED 授权期限已到<br>INVALID_FAILED_BY_AWEME 授权申请失败<br>INVALID_PROCESS_TIME_OUT 超时未处理自动解除<br>INVALID_REJECT C端拒绝授权<br>INVALID_TIME_OUT 超时未确认<br>RENEWING 续期待确认<br>RENEW_FAIL 续期申请失效<br>RENEW_FAILED_BY_AWEME 授权续期申请失败<br>RENEW_SUCCESS 续期成功<br>UNQUALIFIED_AUTO_RELEASE 门槛回扫被清退 |
| data.list[].start_time | string | 授权开始时间 |
| data.list[].share_type | string | 授权共享类型 可选值:<br>SHARE_BY-ACCOUNT 单账户共享<br>SHARE_BY_COMPANY 同主体下账户共享<br>SHARE_BY_EBP_CASCADE_ACCOUNT EBP级联账户共享<br>SHARE_BY_EBP_DIRECT_ACCOUNT EBP直联账户共享<br>SHARE_BY_GROUP 组织下所有账户共享<br>SHARE_BY_ONESELF 投放账户直接授权 |
| data.list[].end_time | string | 授权结束时间 默认值: 2099-01-01 00:00:00 |
| data.list[].video_info | object |  |
| data.list[].video_info.item_id | number(int64) | 抖音视频ID |
| data.list[].video_info.image_mode | string | 素材类型 可选值:<br>AWEME_PHOTO 图文<br>CREATIVE_IMAGE_MODE_AWEME_LIVE 直播画面类型<br>CREATIVE_IMAGE_MODE_DISPLAY_WINDOW 搜索橱窗<br>CREATIVE_IMAGE_MODE_GIF GIF图<br>CREATIVE_IMAGE_MODE_GROUP 组图<br>CREATIVE_IMAGE_MODE_LARGE 大图<br>CREATIVE_IMAGE_MODE_LARGE_VERTICAL 大图竖图<br>CREATIVE_IMAGE_MODE_PLAYABLE_HORIZONTAL 横版试玩素材类型<br>CREATIVE_IMAGE_MODE_PLAYABLE_VERTICAL 竖版试玩素材类型<br>CREATIVE_IMAGE_MODE_SMALL 小图<br>CREATIVE_IMAGE_MODE_UNION_SPLASH 穿山甲开屏图片<br>CREATIVE_IMAGE_MODE_UNION_SPLASH_VIDEO 穿山甲开屏视频<br>CREATIVE_IMAGE_MODE_VIDEO 横版视频<br>CREATIVE_IMAGE_MODE_VIDEO_VERTICAL 竖版视频<br>SEARCH_AD_SMALL_IMAGE 搜索小图<br>TOUTIAO_SEARCH_AD_IMAGE 搜索大图 |
| data.list[].video_info.video_id | string | 视频ID（仅抖音视频会有此字段，抖音图文素材没有此字段） |
| data.list[].video_info.title | string | 视频/图文名称 |
| data.list[].video_info.aweme_play_url | string | 视频/图文播放链接 |
| data.list[].video_info.duration | number(double) | 视频时长，单位为秒<br>（仅抖音视频会有此字段，抖音图文素材没有此字段） |
| data.list[].video_info.video_cover_id | string | 视频封面ID |
| data.list[].video_info.video_cover_url | string | 视频/图文封面预览链接，有效期10小时 |
| data.list[].video_info.mid | number(int64) | 视频素材ID<br>（仅抖音视频会有此字段，抖音图文素材没有此字段） |
| data.list[].note | string | 备注信息，发起抖音授权申请时填写的希望展示给抖音号创作者的备注，创作者可在授权邀请页&授权详情页查看 |
| data.list[].aweme_cancel_reason | string | 抖音号作者发起解除授权的原因，仅当抖音号作者发起解除授权时有值，100字以内，可能包括：<br>不知道该授权是怎么建立的，申请解除授权<br>联系不到对方，无法进行合作沟通，申请解除授权<br>与对方合作到期或者有纠纷，申请解除授权<br>其他情况（作者会填写其他文案给到） |
| data.list[].aweme_cancel_image_list | string[] | 抖音号作者发起解除授权时上传的凭证信息（选填项，抖音号作者可能不填，此时该参数返回为null） |
| data.list[].aweme_cancel_note | string | 抖音号作者发起解除授权时填写的联系方式（选填项，抖音号作者可能不填，此时该参数返回为null） |
| data.list[].warning_content | string[] | 此参数即将下线，请使用 warning_types字段<br>抖音授权关系警告信息，您可根据该信息及时处理，可能返回<br>不达门槛：表示发起授权的抖音号未达到要求，详细未达门槛信息可通过auth_threshold_info获取<br>即将解除：表示抖音号作者已发起解除授权申请，您需要及时联系作者或同意解除 |
| data.list[].warning_types | string[] | 抖音授权关系警告信息类型支持枚举，一条授权关系可能存在 多种 警告类型：<br>UNQUALIFIED 不达要求，表示发起授权的抖音号未达到要求，详细未达门槛信息可通过应答参数中的auth_threshold_info获取<br>UNQUALIFIED_PENDING_EXPIRE 不达要求-即将解除，表示平台定期巡检并解除不符合授权要求的【个人抖音号授权关系】您可按需将您的个人抖音号升级为抖音企业机构账号或督促账号满足要求<br>仅当auth_type= AWEME_ACCOUNT 抖音号授权时，可能会有此警告<br>AUTHOR_REUQUEST_PENDING_EXPIRE 申请解除中，表示抖音号作者已发起解除授权申请，您需要及时联系作者或同意解除 |
| data.list[].auth_auto_expire_date | string | 授权不达要求后自动解除的日期，日期格式yyyy-mm-dd<br>2025年2月中起，平台将分批解除不符合授权要求的【个人抖音号授权关系】，此抖音号未满足授权要求，授权关系将于auth_auto_expire_date被解除。如您仍希望保持授权关系，可按需将个人抖音号升级为抖音企业机构账号或督促账号达到要求，不达要求的具体信息可查看auth_threshold_info参数。说明详见此文档： 【个人抖音号】商业合作授权定期、分批次复核启动通知<br>当抖音号正常时，该结构体下信息会返回为空 |
| data.list[].auth_threshold_info | object |  |
| data.list[].auth_threshold_info.is_audit | boolean | 是否已成年<br>true表示 是<br>false 表示 否，表示未成年，会导致不达门槛 |
| data.list[].auth_threshold_info.is_real_name_cert | boolean | 是否已实名<br>true表示 是<br>false 表示 否，表示未实名，会导致不达门槛 |
| data.list[].auth_threshold_info.is_serious_violation | boolean | 是否严重违规，<br>true表示 严重违规，会导致不达门槛<br>false 表示 没有严重违规 |
| data.list[].auth_threshold_info.is_reached_rise_fans_count | boolean | 有效涨粉数是否达到1000<br>true表示 达到<br>false 表示 否，表示没有达到1000，会导致不达门槛 |
| data.list[].has_video_hp_visibility_limit | boolean | 发布新视频素材到该抖音号下时，视频主页可见性只能设置「仅单次展示可见」，枚举值：<br>true：是<br>false：否，表示无此限制<br>当值返回true时，代表在创建营销时添加新视频素材到该抖音号下投放，视频的主页可见性设置只允许 HIDE_VIDEO_ON_HP 「仅单次展示可见」 |
| data.list[].open_id | string | 抖音号openID，非抖音号开放平台openID |
| data.page_info | object | 分页信息 |
| data.page_info.page | number(int64) | 页数 |
| data.page_info.page_size | number(int64) | 页面大小 |
| data.page_info.total_number | number(int64) | 总数 |
| data.page_info.total_page | number(int64) | 总页数 |
| request_id | string | 请求日志id |

### 备注

- **本接口为 v2 路径**：`/open_api/2/tools/aweme_auth_list/`（GET），不是 v3.0。
- 页面原文：默认仅返回授权关系生效中的数据，如需其他数据请传入 filtering.auth_status；应答 data.list[].video_info 仅当授权关系生效中、单个作品授权时可返回。
- **auth_type 枚举（请求 filtering.auth_type 为 string[]，应答 auth_type 为 string）**：AWEME_ACCOUNT 抖音号授权 / LIVE_ACCOUNT 直播授权 / VIDEO_ITEM 单视频授权（应答处原文「单个作品授权」）/ AWEME_HOMEPAGE 主页作品授权。抖音号授权可在营销下添加新视频素材并发布到所选抖音号投放；主页作品授权不具备此能力。
- **auth_status 枚举（原文拼写）**：AUTHRIZED 授权中 / AUTHRIZING 待授权确认 / INVALID 授权失效。
- **auth_scenario / auth_scenarios**：请求过滤字段为单数 `filtering.auth_scenario`（string），应答字段为复数 `auth_scenarios`（string[]），枚举均为 LIVE 全域-推直播 / VIDEO 全域-推商品。
- filtering.aweme_ids 最长传入 50 个；filtering.item_ids 一次最多 50 个；filtering.dpa_id 为 DPA 商品库id。
- warning_types 枚举（原文拼写）：UNQUALIFIED 不达要求 / UNQUALIFIED_PENDING_EXPIRE 不达要求-即将解除（仅 auth_type=AWEME_ACCOUNT 时可能出现）/ AUTHOR_REUQUEST_PENDING_EXPIRE 申请解除中（REUQUEST 为文档原文拼写）。
- **share_type 拼写差异**：页面正文/描述原文第一项为 `SHARE_BY-ACCOUNT`（连字符），官方结构化 schema enum 数组为 `SHARE_BY_ACCOUNT`（下划线）；其余 SHARE_BY_COMPANY / SHARE_BY_EBP_CASCADE_ACCOUNT / SHARE_BY_EBP_DIRECT_ACCOUNT / SHARE_BY_GROUP / SHARE_BY_ONESELF 两处一致。两种原文均照录。
- sub_status 共 14 个枚举（见字段表）。end_time 默认值 2099-01-01 00:00:00。
- warning_content 即将下线，请使用 warning_types；auth_auto_expire_date：授权不达要求后自动解除的日期（yyyy-mm-dd），抖音号正常时 auth_threshold_info 返回为空。
- auth_threshold_info：is_audit 是否已成年 / is_real_name_cert 是否已实名 / is_serious_violation 是否严重违规 / is_reached_rise_fans_count 有效涨粉数是否达到1000 / has_video_hp_visibility_limit 为 true 时新视频素材主页可见性只允许 HIDE_VIDEO_ON_HP。
- open_id 为抖音号 openID，非抖音号开放平台 openID。video_info.video_cover_url 有效期 10 小时；video_id / duration / mid 仅抖音视频有，抖音图文素材没有。
- 分页：data.page_info{page, page_size, total_number, total_page}。

## /open_api/v3.0/dpa/brand/adv_auth/fuzzy/get/

**获取账户可投品牌列表**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/dpa/brand/adv_auth/fuzzy/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1866516136873355>（doc_id=1866516136873355）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| account_id | number(int64) | 是 | — | 营销账户id |
| brand_name | string | 否 | — | 品牌名称搜索 |
| match_type | string | 条件必填 | EXACT 精确查询；FUZZY 模糊查询 | 检索类型，当入参品牌名称搜索时，条件必填，可选值:<br>EXACT 精确查询<br>FUZZY 模糊查询 |
| brand_data_source_list | string[] | 是 | — | 品牌数据来源，可选值:<br>YUNTU 云图品牌 |
| has_5a_grass_brand | string | 否 | NO 否；YES 是 | 是否支持5a种草，不传默认查询全部，可选值:<br>NO 否<br>YES 是 |
| page | number(int64) | 是 | — | 页码 |
| page_size | number(int64) | 是 | — | 页面大小 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | 品牌列表信息 |
| data.brand_list | object[] | 归一化品牌列表（对应原接口中cdp_brand_id和name） |
| data.brand_list[].merge_brand_id | number(int64) | 归一化品牌id（对应原cdp_brand_id） |
| data.brand_list[].merge_brand_name | string | 归一化品牌名称（对应原cdp_brand_name） |
| data.brand_list[].available_status | string | 归一化品牌状态，可选值:<br>INVALID 不可用<br>VALID 正常 |
| data.brand_list[].yuntu_brand_detail | object | 云图品牌详情 |
| data.brand_list[].yuntu_brand_detail.outer_brand_id | number(int64) | 云图品牌id（对应原brand_name_id，巨量营销平台的品牌字段） |
| data.brand_list[].yuntu_brand_detail.sub_brand_details | object[] | 云图子品牌列表（对应原sub_brand_map，巨量营销平台的产品系列字段） |
| data.brand_list[].yuntu_brand_detail.sub_brand_details[].outer_brand_id | number(int64) | 云图子品牌id |
| data.brand_list[].yuntu_brand_detail.sub_brand_details[].brand_full_name | string | 云图子品牌名称 |
| data.brand_list[].yuntu_brand_detail.has_grass_brand | string | 是否支持5a种草 可选值:<br>NO 否<br>YES 是 |
| data.brand_list[].merge_brand_name_cn | string | 归一化品牌中文名称 |
| data.brand_list[].merge_brand_name_en | string | 归一化品牌英文名称 |
| data.brand_list[].merge_brand_logo_url | string | 归一化品牌 Logo 地址 |
| data.brand_list[].merge_brand_type_list | string[] | 归一化品牌包含的品牌类型列表 |
| data.brand_list[].merge_brand_data_source_list | string[] | 归一化品牌的数据来源汇总列表；按来源筛选后，仍返回该品牌的来源汇总 |
| data.brand_list[].any_ecommerce_brand_id | number(int64) | 电商原始品牌 ID；包含多个电商品牌时返回首个非空 ID，无匹配电商品牌时为空 |
| data.page | number(int64) | 页码 |
| data.page_size | number(int64) | 页面大小 |
| data.total_number | number(int64) | 总数 |
| request_id | string | 请求日志id |

### 备注

- 完整 path：`/open_api/v3.0/dpa/brand/adv_auth/fuzzy/get/`（GET），支持按品牌名称模糊查询。
- **账户参数字段名为 `account_id`（营销账户id），不是 advertiser_id**。
- `brand_data_source_list` 必填，可选值 YUNTU 云图品牌；`match_type` 当传入 brand_name 时条件必填，可选值 EXACT 精确查询 / FUZZY 模糊查询。**类型差异**：页面正文类型列标注 number，官方结构化 schema 标注 string（enum 为 EXACT/FUZZY），两处原文均照录。
- `has_5a_grass_brand` 不传默认查询全部（NO 否 / YES 是）；`page`、`page_size` 均必填。
- 应答 `data.brand_list[]`：merge_brand_id 归一化品牌id（对应原 cdp_brand_id）/ merge_brand_name 归一化品牌名称（对应原 cdp_brand_name）/ available_status（INVALID 不可用 / VALID 正常）/ yuntu_brand_detail.outer_brand_id 云图品牌id（对应原 brand_name_id）/ sub_brand_details[]{outer_brand_id 云图子品牌id, brand_full_name 云图子品牌名称}（对应原 sub_brand_map）/ has_grass_brand（NO/YES）。
- 分页字段 page / page_size / total_number 直接位于 data 下（无 page_info 包装，也无 total_page）。
- 与 create/update 的 brand_info 对应关系：cdp_brand_id←merge_brand_id，cdp_brand_name←merge_brand_name，brand_name_id←yuntu_brand_detail.outer_brand_id，sub_brand_names←sub_brand_details.brand_full_name，sub_brand_name_ids←sub_brand_details.outer_brand_id。

## /open_api/v3.0/project/status/update/

**批量更新项目状态**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/project/status/update/`
- 文档来源：<https://open.oceanengine.com/labels/34/docs/1740941413906432>（doc_id=1740941413906432）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 广告主ID |
| data | object[] | 是 | — |  |
| data[].project_id | number(int64) | 是 | — | 项目ID |
| data[].opt_status | string | 是 | DISABLE 暂停广告；ENABLE 启用广告 | 目标操作，<br>允许值：ENABLE 启用项目、DISABLE暂停项目。<br>对于删除的广告项目不可进行任何操作，否则会报错。 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见【附录-返回码】 |
| message | string | 返回信息,详见【附录-返回码】 |
| data | json | json返回值 |
| data.project_ids | number[] | 更新成功的营销项目ID集合 |
| data.errors | object[] | 更新失败的营销项目列表 |
| data.errors[].project_id | number(int64) | 项目ID |
| data.errors[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- 页面原文：「批量更新 巨量引擎体验版 营销项目的状态」；**不允许修改搜索周期稳投项目的操作状态（项目层级 delivery_type=DURATION）**。
- 请求 `data[]` 长度限制 1~10；opt_status 允许值 ENABLE 启用项目 / DISABLE 暂停项目；对于删除的营销项目不可进行任何操作，否则会报错。
- 应答 `data.project_ids`（number[] 更新成功的营销项目ID集合）、`data.errors[]`{project_id, error_message}。

## /open_api/v3.0/project/roigoal/update/

**批量修改项目ROI系数**

- 方法：`POST`
- 完整请求地址：`https://api.oceanengine.com/open_api/v3.0/project/roigoal/update/`
- 文档来源：<https://open.oceanengine.com/labels/34/docs/1794208148473859>（doc_id=1794208148473859）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |
| Content-Type | string | 是 | 请求消息类型，允许值: application/json |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 投放账户id |
| data | object[] | 是 | — | 批量修改项目ROI系数，list长度限制10 |
| data[].project_id | number(int64) | 是 | — | 项目ID |
| data[].roi_goal | number(float) | 是 | — | 深度转化ROI系数，填写要求如下：<br>应用、自动投放项目，ROI系数范围[0.01,5]，仅支持最多4位小数<br>电商投放、自动投放项目，ROI系数范围[0.01,100]，仅支持最多4位小数<br>电商投放、自动投放、周期稳投项目，ROI系数只允许调小且新建7天内每自然日最多允许成功修改1次<br>小程序、自动投放项目，ROI系数范围[0.01,5]，仅支持最多4位小数 |
| data[].shop_multi_roi_goals | object[] | 否 | — | 电商平台多ROI系数设置，引流电商多平台投放ROI系数及平台信息，可按照电商平台分别确定ROI系数，分平台调控出价 ，此能力白名单开放 。list长度最长为4<br>填写条件：仅当满足以下5个条件时可传入，此时无需传入roi_goal参数：<br>landing_type = SHOP电商<br>delivery_mode = PROCEDURAL自动投放<br>external_action = AD_CONVERT_TYPE_APP_ORDER APP内下单<br>deep_bid_type = ROI_DIRECT_MAILROI直投<br>投放账户已经开通多ROI投放白名单，如有疑问请联系对接销售/运营<br>填写要求：<br>长度1-4<br>单ROI（仅设置roi_goal）和多ROI项目（仅设置shop_multi_roi_goals）不支持同时传入修改 |
| data[].shop_multi_roi_goals[].roi_goal | number(double) | 否 | — | ROI系数值，范围[0.01,100]，精度：最多保留小数点后四位 |
| data[].shop_multi_roi_goals[].shop_platform | string | 否 | JD 京东；OTHER 其他；PDD 拼多多；TB 淘宝 | 可选值:<br>JD 京东<br>OTHER 其他<br>PDD 拼多多<br>TB 淘宝 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.project_ids | number[] | 更新成功的项目ID列表 |
| data.errors | object[] | 更新失败的项目ID列表及失败原因 |
| data.errors[].project_id | number(int64) | 项目ID |
| data.errors[].error_message | string | 错误信息 |
| request_id | string | 请求日志id |

### 备注

- 页面原文：当前仅以下项目支持 ROI 系数修改——应用、自动投放项目；电商、自动投放项目；电商、自动投放、周期稳投项目；小程序、自动投放项目。
- 请求 `data[]` 长度限制 10；`roi_goal`：应用/小程序自动投放项目范围 [0.01,5]，电商自动投放项目范围 [0.01,100]，均仅支持最多 4 位小数；电商周期稳投项目 ROI 系数只允许调小，且新建 7 天内每自然日最多允许成功修改 1 次。
- `shop_multi_roi_goals`（电商平台多ROI系数，白名单开放）：list 长度 1-4；仅当 landing_type=SHOP、delivery_mode=PROCEDURAL、external_action=AD_CONVERT_TYPE_APP_ORDER、deep_bid_type=ROI_DIRECT_MAIL 且账户已开通多ROI投放白名单时可传入，此时无需传入 roi_goal；单 ROI（仅 roi_goal）与多 ROI（仅 shop_multi_roi_goals）不支持同时传入修改。
- `shop_multi_roi_goals[].roi_goal` 范围 [0.01,100]，最多 4 位小数；`shop_platform` 可选值 JD 京东 / OTHER 其他 / PDD 拼多多 / TB 淘宝。
- 应答 `data.project_ids`（更新成功的项目ID列表）、`data.errors[]`{project_id, error_message}。

## /open_api/2/tools/log_search/

**操作日志查询**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/2/tools/log_search/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1696710682956815>（doc_id=1696710682956815）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 投放账户id |
| object_id | number[] | 否 | — | 操作对象ID，可以为campaign_id、ad_id、creative_id等业务下对应对象ID<br>长度限制：[1,20] |
| start_time | string | 否 | — | 日志查询开始时间<br>格式：yyyy-mm-dd HH:MM:SS |
| end_time | string | 否 | — | 日志查询结束时间<br>格式：yyyy-mm-dd HH:MM:SS |
| page | number(int64) | 否 | — | 页码 |
| page_size | number(int64) | 否 | — | 页面大小 |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.logs | object[] | 日志详情 |
| data.logs[].content_log | string[] | 操作前后内容 |
| data.logs[].content_title | string | 操作内容 |
| data.logs[].create_time | string | 操作时间 |
| data.logs[].object_id | number(int64) | 操作对象id |
| data.logs[].object_name | string | 操作对象名称 |
| data.logs[].object_type | string | 操作对象裂隙 |
| data.logs[].operator | string | 操作人 |
| data.logs[].opt_ip | string | 操作ip |
| data.logs[].second_log_id | number(int64) | 大日志id |
| data.page_info | object | 分页信息 |
| data.page_info.page | number(int64) | 页码 |
| data.page_info.page_size | number(int64) | 页面大小 |
| data.page_info.total_number | number(int64) | 总数。<br>当前一次最多支持查询2000条日志数据，超过2000条返回的分页信息可能存在不准确的问题，建议您缩短查询时间（7天以内）。 |
| data.page_info.total_page | number(int64) | 总页数<br>当前一次最多支持查询2000条日志数据，超过2000条返回的分页信息可能存在不准确的问题，建议您缩短查询时间（7天以内）。 |
| request_id | string | 请求日志id |

### 备注

- **本接口为 v2 路径**：`/open_api/2/tools/log_search/`（GET）。
- 页面原文：可查询巨量营销、巨量本地推、巨量千川后台操作日志，默认查询最近 7 天的数据，最多查询跨度为一个月；一次最多支持查询 2000 条日志数据，超过 2000 条返回的分页信息可能不准确，建议缩短查询时间（7 天以内）。
- `object_id`（number[]）可为 campaign_id、ad_id、creative_id 等业务下对应对象ID，长度限制 [1,20]；start_time / end_time 格式 yyyy-mm-dd HH:MM:SS。
- **大日志**：页面正文写「当应答参数中返回 search_log_id 时……详细信息需调用【获取操作日志详情】接口获取」，但应答字段表中实际字段名为 `second_log_id`（number，大日志id）——search_log_id 未出现在字段表（见未核实清单）。
- `logs[].object_type` 描述原文为「操作对象裂隙」（疑似「类型」笔误，照录）。
- 分页：data.page_info{page, page_size, total_number, total_page}，total_number/total_page 均附 2000 条上限说明。

## /open_api/2/tools/log_search/detail/get/

**获取操作日志详情**

- 方法：`GET`
- 完整请求地址：`https://api.oceanengine.com/open_api/2/tools/log_search/detail/get/`
- 文档来源：<https://open.oceanengine.com/labels/7/docs/1854930515444873>（doc_id=1854930515444873）

### Header

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Access-Token | string | 是 | 授权access_token，可以通过【获取Access token】接口获取 |

### 请求参数

| 字段 | 类型 | 必填 | 枚举值 | 说明 |
| --- | --- | --- | --- | --- |
| advertiser_id | number(int64) | 是 | — | 客户id |
| second_log_id | number(int64) | 是 | — | 大日志id |

### 应答参数

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码,详见 【附录-返回码】 |
| message | string | 返回信息,详见 【附录-返回码】 |
| data | json | json返回值 |
| data.log | object |  |
| data.log.content_title | string | 操作内容 |
| data.log.object_type | string | 操作对象类型 |
| data.log.object_id | number(int64) | 操作对象ID |
| data.log.object_name | string | 操作对象名称 |
| data.log.create_time | string | 操作时间 |
| data.log.content_log | string[] | 操作前后内容 |
| data.log.operator | string | 操作人 |
| data.log.opt_ip | string | 操作IP |
| request_id | string | 请求日志id |

### 备注

- **本接口为 v2 路径**：`/open_api/2/tools/log_search/detail/get/`（GET）。
- `second_log_id`（大日志id）必填，来自 log_search 应答 logs[].second_log_id。
- 应答 `data.log`{content_title, object_type, object_id, object_name, create_time, content_log[], operator, opt_ip}。

## 未核实清单

16 个目标接口的契约（方法、path、请求/应答字段表、枚举、页面语义说明）**全部核实成功**，无「页面无法抓取」项。
以下为文档未说明或本次未抓取的内容，如实标注：

1. **请求/应答示例代码**（各页 Python / Java / Php / Curl 标签页）：示例区为前端动态加载，静态正文仅含标签标题，未抓取示例文本。
2. **uni_project/update 的空值语义**：页面仅声明「接口的更新逻辑为增量更新」，未解释传 null / 空字符串 / 空数组时字段是否保持原值或被清空。未核实。
3. **log_search 的 `search_log_id`**：页面正文提及该名称，但应答字段表只有 `second_log_id`（大日志id）。`search_log_id` 作为独立字段未核实，本文按字段表以 `second_log_id` 为准。
4. **【获取账户可投行业信息】接口契约**（create.brand_info.yuntu_category_id 的取值来源）：不在本次 16 个 URL 范围内，未核实。
5. **【查询白名单能力】（gray_key=R3_brand）、【获取关联云图的投放账户信息】、【获取全域创编可用抖音图文素材】等配合接口**：正文引用，契约不在本次范围内，未核实。
6. **限流错误码的出处**：16 个接口页面本身均未列出错误码表，只写「详见【附录-返回码】」。下节错误码抓取自该官方附录文档（doc_id=1696710760866831，标题「返回码」），已核实。
7. **WebFetch 直抓**：16 个 URL 均只返回 JS 外壳无正文，`?origin=left_nav` 重试同样失败；doc/index.html 搜索入口未再使用——因已发现页面自身调用的官方 skiff 文档数据 API，16 个文档全部经该 API 同源取得。`markdown/get` 端点一次请求未落盘（未重试），不影响契约完整性（schema + node 两源已覆盖）。

## 通用封套 / 错误码

### 通用应答封套（16 个接口一致，逐页核实）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 返回码，详见【附录-返回码】；0 = OK（请求成功） |
| message | string | 返回信息，详见【附录-返回码】 |
| data | json | json 返回值（各接口业务字段见上文；material/delete 的 data 无业务字段） |
| request_id | string | 请求日志id |

- GET 请求参数以 query 形式传入，POST 请求以 body 形式传入（附录 40000 排查建议原文）。
- 批量类接口（monetization_mode/update、material/status/update、project/status/update、project/roigoal/update）采用「成功集合 + errors 明细」的部分成功语义；create/update/material/create 采用 error_list 明细。

### 错误码（来源：官方【附录-返回码】文档，doc_id=1696710760866831）

**成功返回**

| code | msg | 排查建议 |
| --- | --- | --- |
| 0 | OK | 请求成功 |

**限流错误码（重点）**

| code | msg（原文） | 排查建议（原文摘要） |
| --- | --- | --- |
| 40100 | 系统请求频率超限，请稍后重试。历史报错文案：Too many requests. Please retry in some time. | 系统级别限流：统计单位时间内接口下所有开发者的请求次数是否超限。当前处于接口流量高峰期，建议稍后再试 |
| 40110 | 您的开发者账户请求频率超限，请稍后重试。历史报错文案：Too many requests by this developer. Please retry in some time. | 开发者秒级（QPS）限流管控：开发者账户在该接口的请求次数超出开发者 QPS 配额。建议登录开发者管理后台查询配额及优化建议；检查自身限流器是否为滑动窗口算法。详见「频控限制」说明文档 |
| 40130 | 当前客户（advertiser_id）的可用QPS已达上限。请避免立即重试，建议您优先请求近期活跃/活跃的客户，或降低当前客户的请求频率 | 每个客户（advertiser_id）的 QPS 配额基于历史活跃度和服务需求动态分配，不支持申报提额。参考解决方案：https://open.oceanengine.com/labels/7/docs/1834161681551499 |

**通用返回**（附录原文：4 开头错误码均为正常业务校验报错，如参数类型不符合要求，或不符合业务要求）

| code | msg（原文） | 排查建议（原文摘要） |
| --- | --- | --- |
| 40000 | The advertiser ID or the account information is invalid. Please check and retry. | 传入 advertiser_id 错误，或不符合接口要求类型，注意查看接口要求传入的账户类型为客户还是其他类型 |
| 40000 | xx : Missing data for required field. | xx 为必填字段，请注意查看接口文档字段描述要求 |
| 40000 | xx: value is required but missing | 未传入必填字段 xx |
| 40000 | xx: Value must be an yy | 传入字段 xx 不符合要求类型 yy |
| 40000 | xx: Field must be set to yy | 传入字段 xx 不符合要求类型 yy |
| 40000 | xx: Value is not nullable | xx 字段内容不能为空，如非必填字段，不使用不要传入字段 |
| 40000 | The advertiser ID is required. Please provide it and retry. | 未传入必填 advertiser_id；get 请求以 query 形式传入，post 请求以 body 形式传入 |
| 40002 | No permission to operate account xxx | Access_Token 下无对应 xxx 账户授权，通过【获取已授权账号】接口确认 |
| 40002 | advertiser does no grant xxx permission | 申请授权时未向客户申请对应接口 xxx 授权，可重新申请客户授权对应功能 |
| 40002 | app_secret 不匹配，无权限操作当前应用 | 传入 secret 与 app_id 不一致，请登陆开发者后台查询应用信息确认 |
| 40102 | access_token已过期 | 传入 Access_Token 已失效，请重新获取 |
| 40103 | refresh_token已过期 | refresh_token 已被使用，或授权账号重新授权并生成了新的 Token |
| 40104 | The access_token is empty. | 传入 Access_Token 无效，请重新获取正确 Access_Token 传入 |
| 40107 | refresh_token无效，请传入最新的refresh_token | refresh_token 已被使用，或授权账号重新授权并生成了新的 Token |
| 40113 | App不存在或者状态异常 | Access_Token 对应的开发者应用状态异常，请登陆开发者后台确认应用是否存在以及状态是否启用 |
| 40115 | 授权码无效 | auth_code 已失效：1. 已使用 2. 已过 10 分钟有效期 3. 同账号重新授权生成 |
| 40115 | auth_code已经被使用，请重新授权 | 同上 |
| 40118 | The API is not in allow list. Please apply for the allow list and retry. | 接口未上线，Access_Token 对应开发者无对应接口请求权限 |
| 40119 | The developer and the advertiser do not belong to the same group. Please contact the administrator. | 线索、评论等敏感物料接口存在主体校验；如需操作需在申请授权时同时申请敏感物料授权（授权链接拼接 material_auth=1） |

**系统相关**

| code | msg | 排查建议 |
| --- | --- | --- |
| 50000 | SYS_ERROR | 系统请求超时，可以重试 |

