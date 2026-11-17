package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.Advertiser;
import com.unidelivery.app.domain.AwemeAuth;
import com.unidelivery.app.domain.PrecheckRecord;
import com.unidelivery.app.domain.ProductPlaylet;
import com.unidelivery.app.dto.PreCheckItem;
import com.unidelivery.app.dto.PreCheckResult;
import com.unidelivery.app.repo.AdvertiserRepository;
import com.unidelivery.app.repo.AwemeAuthRepository;
import com.unidelivery.app.repo.PrecheckRecordRepository;
import com.unidelivery.app.repo.ProductPlayletRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 投前预检闸门：白名单 / 抖音号全域授权 / 创编可用抖音号 / 商品剧目 / 品牌摸底 / 唯一标的。
 * 任一项读不到或失败即 fail-closed；结果结构化落库留痕。
 */
@Service
public class PreCheckService {

    private static final Logger log = LoggerFactory.getLogger(PreCheckService.class);
    private static final int MAX_PAGES = 20;

    private final MapiClient mapi;
    private final AdvertiserRepository advertisers;
    private final AwemeAuthRepository awemeAuths;
    private final ProductPlayletRepository products;
    private final UniProjectRepository projects;
    private final PrecheckRecordRepository records;

    public PreCheckService(MapiClient mapi, AdvertiserRepository advertisers, AwemeAuthRepository awemeAuths,
                           ProductPlayletRepository products, UniProjectRepository projects,
                           PrecheckRecordRepository records) {
        this.mapi = mapi;
        this.advertisers = advertisers;
        this.awemeAuths = awemeAuths;
        this.products = products;
        this.projects = projects;
        this.records = records;
    }

    @Transactional
    public PreCheckResult run(long advertiserId, long awemeId, long assetId, Long playletId) {
        List<PreCheckItem> items = new ArrayList<>();
        PreCheckItem whitelist = checkWhitelist(advertiserId);
        items.add(whitelist);
        if (whitelist.passed()) {
            items.add(checkAwemeAuth(advertiserId, awemeId));
            items.add(checkAwemeAvailable(advertiserId, awemeId, playletId));
            items.add(checkBrand(advertiserId));
        } else {
            // 白名单未确认：不向平台发起任何调用（fail-closed），远程项记为未校验
            items.add(PreCheckItem.fail("AWEME_AUTH", "白名单未通过，未发起远程校验", null));
            items.add(PreCheckItem.fail("AWEME_AVAILABLE", "白名单未通过，未发起远程校验", null));
            items.add(PreCheckItem.fail("BRAND", "白名单未通过，未发起远程校验", null));
        }
        items.add(checkProduct(advertiserId, assetId, playletId));
        items.add(checkTarget(awemeId, assetId));
        for (PreCheckItem item : items) {
            PrecheckRecord row = new PrecheckRecord();
            row.advertiserId = advertiserId;
            row.awemeId = awemeId;
            row.assetId = assetId;
            row.item = item.item();
            row.passed = item.passed();
            row.reason = item.reason();
            row.evidence = item.evidence();
            row.createdAt = LocalDateTime.now();
            records.save(row);
        }
        return new PreCheckResult(items);
    }

    private PreCheckItem checkWhitelist(long advertiserId) {
        return advertisers.findById(advertiserId)
                .filter(a -> a.whitelisted && a.whitelistEvidence != null && !a.whitelistEvidence.isBlank())
                .map(a -> PreCheckItem.pass("WHITELIST", "账户在白名单内", a.whitelistEvidence))
                .orElseGet(() -> PreCheckItem.fail("WHITELIST",
                        "账户未确认在全域投放白名单内（邀请制，需平台运营确认并留存记录）", null));
    }

    /** 授权巡检入口（定时任务用）：只读校验并刷新本地授权关系。 */
    @Transactional
    public PreCheckItem patrolAuth(long advertiserId, long awemeId) {
        return checkAwemeAuth(advertiserId, awemeId);
    }

    private PreCheckItem checkAwemeAuth(long advertiserId, long awemeId) {
        try {
            Map<String, Object> filtering = new LinkedHashMap<>();
            filtering.put("auth_scenario", "VIDEO");
            filtering.put("auth_status", List.of("AUTHRIZED"));
            filtering.put("aweme_ids", List.of(String.valueOf(awemeId)));
            JsonNode data = mapi.call(MapiEndpoint.AWEME_AUTH_LIST, advertiserId,
                    Map.of("advertiser_id", advertiserId, "filtering", filtering, "page", 1, "page_size", 50),
                    JsonNode.class);
            JsonNode match = null;
            for (JsonNode entry : data.path("list")) {
                if (String.valueOf(awemeId).equals(entry.path("aweme_id").asText())
                        && "AUTHRIZED".equals(entry.path("auth_status").asText())) {
                    match = entry;
                    break;
                }
            }
            if (match == null) {
                return PreCheckItem.fail("AWEME_AUTH", "抖音号无生效中的全域授权（auth_status=AUTHRIZED）", null);
            }
            boolean videoScenario = false;
            for (JsonNode scenario : match.path("auth_scenarios")) {
                if ("VIDEO".equals(scenario.asText())) {
                    videoScenario = true;
                }
            }
            AwemeAuth row = awemeAuths.findByAdvertiserIdAndAwemeId(advertiserId, awemeId)
                    .orElseGet(() -> {
                        AwemeAuth created = new AwemeAuth();
                        created.advertiserId = advertiserId;
                        created.awemeId = awemeId;
                        return created;
                    });
            row.authScenarios = match.path("auth_scenarios").toString();
            row.uniAuthorized = videoScenario;
            row.checkedAt = LocalDateTime.now();
            awemeAuths.save(row);
            if (!videoScenario) {
                return PreCheckItem.fail("AWEME_AUTH",
                        "授权生效但不含全域-推商品（auth_scenarios 缺 VIDEO）", row.authScenarios);
            }
            return PreCheckItem.pass("AWEME_AUTH", "全域投放授权生效（VIDEO）",
                    "auth_type=" + match.path("auth_type").asText());
        } catch (MapiException e) {
            return PreCheckItem.fail("AWEME_AUTH", "授权关系读取失败: " + e.getMessage(), e.requestId());
        }
    }

    private PreCheckItem checkAwemeAvailable(long advertiserId, long awemeId, Long playletId) {
        if (playletId == null) {
            return PreCheckItem.fail("AWEME_AVAILABLE", "缺少专辑ID（playlet_id），无法校验创编可用抖音号", null);
        }
        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                JsonNode data = mapi.call(MapiEndpoint.UNI_AWEME_AUTHORIZED, advertiserId,
                        Map.of("advertiser_id", advertiserId, "playlet_id", playletId, "page", page),
                        JsonNode.class);
                for (JsonNode entry : data.path("aweme_id_list")) {
                    if (String.valueOf(awemeId).equals(entry.path("aweme_id").asText())) {
                        return PreCheckItem.pass("AWEME_AVAILABLE", "抖音号在全域创编可用列表内",
                                "playlet_id=" + playletId + " page=" + page);
                    }
                }
                if (!data.path("has_more").asBoolean(false)) {
                    break;
                }
            }
            return PreCheckItem.fail("AWEME_AVAILABLE",
                    "抖音号不在该专辑的全域创编可用列表内", "playlet_id=" + playletId);
        } catch (MapiException e) {
            return PreCheckItem.fail("AWEME_AVAILABLE", "创编可用抖音号读取失败: " + e.getMessage(), e.requestId());
        }
    }

    private PreCheckItem checkProduct(long advertiserId, long assetId, Long playletId) {
        ProductPlaylet row = products.findByAdvertiserIdAndAssetId(advertiserId, assetId).orElse(null);
        if (row == null) {
            return PreCheckItem.fail("PRODUCT",
                    "商品未录入/未同步（需升级版短剧漫剧库商品，含有效专辑ID）", null);
        }
        if (!"UPGRADED".equals(row.libraryVersion)) {
            return PreCheckItem.fail("PRODUCT", "商品不在升级版短剧漫剧库（旧版库不可投）", row.libraryVersion);
        }
        if (!row.deliverable) {
            return PreCheckItem.fail("PRODUCT", "商品状态不可投放（status/audit_status 未双通过）",
                    "checked_at=" + row.checkedAt);
        }
        if (row.playletId == null) {
            return PreCheckItem.fail("PRODUCT", "商品缺少有效专辑ID（playlet_id）", null);
        }
        if (playletId != null && !playletId.equals(row.playletId)) {
            return PreCheckItem.fail("PRODUCT", "传入专辑ID与商品绑定专辑不一致",
                    "cmd=" + playletId + " product=" + row.playletId);
        }
        return PreCheckItem.pass("PRODUCT", "升级版库商品可投放且专辑ID有效",
                "product_id=" + assetId + " playlet_id=" + row.playletId);
    }

    private PreCheckItem checkBrand(long advertiserId) {
        try {
            JsonNode data = mapi.call(MapiEndpoint.BRAND_LIST, advertiserId,
                    Map.of("account_id", advertiserId, "brand_data_source_list", List.of("YUNTU"),
                            "page", 1, "page_size", 50),
                    JsonNode.class);
            JsonNode brandList = data.path("brand_list");
            if (!brandList.isArray() || brandList.isEmpty()) {
                return PreCheckItem.pass("BRAND", "账户无可投品牌，brand_info 无需传入", "brand_list=[]");
            }
            JsonNode brand = null;
            for (JsonNode candidate : brandList) {
                if ("VALID".equals(candidate.path("available_status").asText())) {
                    brand = candidate;
                    break;
                }
            }
            if (brand == null) {
                return PreCheckItem.fail("BRAND", "账户品牌均不可用（available_status=INVALID）", brandList.toString());
            }
            Map<String, Object> brandInfo = assembleBrandInfo(brand);
            Advertiser advertiser = advertisers.findById(advertiserId).orElseThrow();
            advertiser.brandInfoJson = toJson(brandInfo);
            advertiser.brandCheckedAt = LocalDateTime.now();
            advertiser.updatedAt = LocalDateTime.now();
            advertisers.save(advertiser);
            return PreCheckItem.pass("BRAND", "账户存在可投品牌，brand_info 已装配（条件必填）",
                    "merge_brand_id=" + brand.path("merge_brand_id").asLong());
        } catch (MapiException e) {
            return PreCheckItem.fail("BRAND", "品牌列表读取失败（读不到即 fail）: " + e.getMessage(), e.requestId());
        }
    }

    /** 契约核实的取值映射：cdp_brand_id←merge_brand_id 等；yuntu_category_id 来源接口未核实，缺省不传。 */
    public static Map<String, Object> assembleBrandInfo(JsonNode brand) {
        Map<String, Object> brandInfo = new LinkedHashMap<>();
        brandInfo.put("cdp_brand_id", brand.path("merge_brand_id").asLong());
        brandInfo.put("cdp_brand_name", brand.path("merge_brand_name").asText());
        JsonNode yuntu = brand.path("yuntu_brand_detail");
        if (yuntu.has("outer_brand_id")) {
            brandInfo.put("brand_name_id", yuntu.path("outer_brand_id").asLong());
        }
        List<String> subNames = new ArrayList<>();
        List<String> subIds = new ArrayList<>();
        for (JsonNode sub : yuntu.path("sub_brand_details")) {
            subNames.add(sub.path("brand_full_name").asText());
            subIds.add(sub.path("outer_brand_id").asText());
        }
        if (!subNames.isEmpty()) {
            brandInfo.put("sub_brand_names", subNames);
            brandInfo.put("sub_brand_name_ids", subIds);
        }
        return brandInfo;
    }

    private PreCheckItem checkTarget(long awemeId, long assetId) {
        return projects.findByAwemeIdAndAssetIdAndActiveFlag(awemeId, assetId, 1)
                .map(p -> PreCheckItem.fail("TARGET",
                        "标的已存在活跃全域项目（唯一标的规则）", "project_id=" + p.projectId))
                .orElseGet(() -> PreCheckItem.pass("TARGET", "标的无活跃全域项目", null));
    }

    private String toJson(Map<String, Object> value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("brand_info 序列化失败", e);
        }
    }
}
