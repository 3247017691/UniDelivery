package com.unidelivery.mapi;

/**
 * MAPI 接口注册表。defaultPath 为手册/官方文档记载值；null 表示 path 待契约核实，
 * 必须通过配置 mapi.paths.&lt;KEY&gt; 覆盖后才可调用（防幻觉 path 进入生产）。
 */
public enum MapiEndpoint {
    UNI_PROJECT_CREATE("/open_api/v3.0/uni_project/create/", Http.POST, true),
    UNI_PROJECT_UPDATE("/open_api/v3.0/uni_project/update/", Http.POST, true),
    UNI_PROJECT_LIST("/open_api/v3.0/uni_project_list/get/", Http.GET, false),
    OC_REJECT_REASON("/open_api/v3.0/oc_project/reject_reason/get/", Http.GET, false),
    UNI_AWEME_AUTHORIZED("/open_api/v3.0/uni_project/aweme/authorized/get/", Http.GET, false),
    UNI_MONETIZATION_UPDATE("/open_api/v3.0/uni_project/monetization_mode/update/", Http.POST, true),

    PROJECT_STATUS_UPDATE("/open_api/v3.0/project/status/update/", Http.POST, true),
    PROJECT_DELETE("/open_api/v3.0/project/delete/", Http.POST, true),
    PROJECT_SCHEDULE_TIME_UPDATE("/open_api/v3.0/project/schedule_time/update/", Http.POST, true),
    PROJECT_WEEK_SCHEDULE_UPDATE("/open_api/v3.0/project/week_schedule/update/", Http.POST, true),
    PROJECT_ROIGOAL_UPDATE("/open_api/v3.0/project/roigoal/update/", Http.POST, true),

    OC_MATERIAL_GET("/open_api/v3.0/oc_project/material/get/", Http.GET, false),
    OC_MATERIAL_CREATE("/open_api/v3.0/oc_project/material/create/", Http.POST, true),
    OC_MATERIAL_DELETE("/open_api/v3.0/oc_project/material/delete/", Http.POST, true),
    OC_MATERIAL_STATUS_UPDATE("/open_api/v3.0/oc_project/material/status/update/", Http.POST, true),

    VIDEO_UPLOAD("/open_api/2/file/video/ad/", Http.MULTIPART, true),
    VIDEO_UPDATE("/open_api/2/file/video/update/", Http.POST, true),
    VIDEO_GET("/open_api/2/file/video/get/", Http.GET, false),
    UPLOAD_TASK_CREATE("/open_api/2/file/upload_task/create/", Http.POST, true),
    /** 异步上传结果轮询（契约核实：upload_task/list，状态 PROCESS/SUCCESS/FAILED）。 */
    UPLOAD_TASK_GET("/open_api/2/file/video/upload_task/list/", Http.GET, false),
    /** SPI 补拉（契约核实：host 为 ad.oceanengine.com，APP-Access-Token）。 */
    SPI_TASK_GET("/open_api/2/spi_task/get/", Http.GET, false),

    REPORT_CONFIG_GET("/open_api/v3.0/report/custom/config/get/", Http.GET, false),
    REPORT_CUSTOM_GET("/open_api/v3.0/report/custom/get/", Http.GET, false),

    AWEME_AUTH_LIST("/open_api/2/tools/aweme_auth_list/", Http.GET, false),
    AWEME_VIDEO_GET("/open_api/2/file/video/aweme/get/", Http.GET, false),
    LOG_SEARCH("/open_api/2/tools/log_search/", Http.GET, false),
    LOG_DETAIL("/open_api/2/tools/log_search/detail/get/", Http.GET, false),
    /** 获取账户可投品牌列表（契约核实：账户参数为 account_id）。 */
    BRAND_LIST("/open_api/v3.0/dpa/brand/adv_auth/fuzzy/get/", Http.GET, false),
    /** 升级版短剧漫剧库商品查询（契约核实：account_id=升级版组织ID，account_type=EBP）。 */
    PRODUCT_LIST("/open_api/v3.0/dpa/ebp/product/list/", Http.GET, false),
    PRODUCT_GET("/open_api/v3.0/dpa/ebp/product/detail/get/", Http.GET, false),

    COMMENT_GET("/open_api/v3.0/tools/comment/get/", Http.GET, false),
    COMMENT_REPLY_GET("/open_api/v3.0/tools/comment_reply/get/", Http.GET, false),
    COMMENT_REPLY("/open_api/v3.0/tools/comment/reply/", Http.POST, true),
    COMMENT_METRICS("/open_api/v3.0/tools/comment_metrics/get/", Http.GET, false),
    COMMENT_HIDE("/open_api/v3.0/tools/comment/hide/", Http.POST, true),
    COMMENT_MID2ITEM("/open_api/v3.0/tools/comment/mid2item_id/", Http.GET, false);

    public enum Http { GET, POST, MULTIPART }

    private final String defaultPath;
    private final Http method;
    private final boolean write;

    MapiEndpoint(String defaultPath, Http method, boolean write) {
        this.defaultPath = defaultPath;
        this.method = method;
        this.write = write;
    }

    public String defaultPath() {
        return defaultPath;
    }

    public Http method() {
        return method;
    }

    public boolean write() {
        return write;
    }

    /** 个别接口官方文档 host 为 ad.oceanengine.com（契约核实），其余走配置 baseUrl。 */
    public String host() {
        return switch (this) {
            case AWEME_VIDEO_GET, SPI_TASK_GET -> "https://ad.oceanengine.com";
            default -> null;
        };
    }
}
