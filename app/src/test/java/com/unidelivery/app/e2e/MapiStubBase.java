package com.unidelivery.app.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.unidelivery.app.domain.Advertiser;
import com.unidelivery.app.domain.OauthToken;
import com.unidelivery.app.repo.AdvertiserRepository;
import com.unidelivery.app.repo.OauthTokenRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * 端到端 mock 联调基座：WireMock 扮演 MAPI（契约形状来自 docs/contracts-*.md），
 * H2 扮演业务库。所有 stub 的字段名/枚举与官方契约一致（含 material_status_frist 拼写）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class MapiStubBase {

    protected static final WireMockServer MAPI = new WireMockServer(WireMockConfiguration.options().port(8089));
    protected static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    protected TestRestTemplate rest;
    @Autowired
    protected AdvertiserRepository advertisers;
    @Autowired
    protected OauthTokenRepository tokens;

    @BeforeAll
    static void startMapi() {
        if (!MAPI.isRunning()) {
            MAPI.start();
        }
    }

    @AfterAll
    static void stopMapi() {
        if (MAPI.isRunning()) {
            MAPI.stop();
        }
    }

    @BeforeEach
    void resetStubs() {
        MAPI.resetAll();
    }

    protected void seedAdvertiser(long advertiserId) {
        if (advertisers.findById(advertiserId).isEmpty()) {
            Advertiser advertiser = new Advertiser();
            advertiser.advertiserId = advertiserId;
            advertiser.name = "e2e-adv";
            advertiser.whitelisted = true;
            advertiser.whitelistEvidence = "平台运营确认记录 PR-E2E";
            advertiser.createdAt = LocalDateTime.now();
            advertiser.updatedAt = LocalDateTime.now();
            advertisers.save(advertiser);
        }
        if (tokens.findById(advertiserId).isEmpty()) {
            OauthToken token = new OauthToken();
            token.advertiserId = advertiserId;
            token.accessToken = "e2e-token";
            token.refreshToken = "e2e-refresh";
            token.expiresAt = LocalDateTime.now().plusHours(10);
            token.refreshExpiresAt = LocalDateTime.now().plusDays(20);
            token.updatedAt = LocalDateTime.now();
            tokens.save(token);
        }
    }

    protected static String envelope(String dataJson) {
        return "{\"code\":0,\"message\":\"OK\",\"data\":" + dataJson + ",\"request_id\":\"stub-req\"}";
    }

    protected void stubGet(String path, String dataJson) {
        MAPI.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody(envelope(dataJson))));
    }

    protected void stubPost(String path, String dataJson) {
        MAPI.stubFor(post(urlPathEqualTo(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody(envelope(dataJson))));
    }

    protected void stubAuth(List<String> awemeIds) {
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < awemeIds.size(); i++) {
            if (i > 0) {
                list.append(',');
            }
            list.append("{\"aweme_id\":\"").append(awemeIds.get(i))
                    .append("\",\"auth_status\":\"AUTHRIZED\",\"auth_scenarios\":[\"VIDEO\"],")
                    .append("\"auth_type\":\"AWEME_ACCOUNT\"}");
        }
        list.append(']');
        stubGet("/open_api/2/tools/aweme_auth_list/", "{\"list\":" + list + ",\"page_info\":{\"total_page\":1}}");
    }

    protected void stubAuthorized(List<String> awemeIds) {
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < awemeIds.size(); i++) {
            if (i > 0) {
                list.append(',');
            }
            list.append("{\"aweme_id\":\"").append(awemeIds.get(i)).append("\"}");
        }
        list.append(']');
        stubGet("/open_api/v3.0/uni_project/aweme/authorized/get/",
                "{\"aweme_id_list\":" + list + ",\"has_more\":false}");
    }

    protected void stubBrandEmpty() {
        stubGet("/open_api/v3.0/dpa/brand/adv_auth/fuzzy/get/",
                "{\"brand_list\":[],\"page\":1,\"page_size\":50,\"total_number\":0}");
    }

    protected void stubProduct(long productId, String playletId, int status, int auditStatus) {
        stubGet("/open_api/v3.0/dpa/ebp/product/list/",
                "{\"list\":[{\"product_id\":" + productId + ",\"status\":" + status
                        + ",\"audit_status\":" + auditStatus + ",\"audit_reason_text\":\"\","
                        + "\"profession\":{\"playlet_id\":\"" + playletId + "\",\"album_id\":\"8001\"}}],"
                        + "\"page_info\":{\"total_page\":1}}");
    }

    protected void stubCreate(long projectId, String errorList) {
        stubPost("/open_api/v3.0/uni_project/create/",
                "{\"project_id\":" + projectId + ",\"error_list\":" + errorList + "}");
    }

    protected void stubMaterialCreate(String errorList) {
        stubPost("/open_api/v3.0/oc_project/material/create/", "{\"error_list\":" + errorList + "}");
    }

    protected void stubMaterialGet(String videoListJson) {
        stubGet("/open_api/v3.0/oc_project/material/get/",
                "{\"video_material_list\":" + videoListJson + ",\"cursor_info\":{\"has_more\":false}}");
    }

    protected void stubUpdate(String errorList) {
        stubPost("/open_api/v3.0/uni_project/update/",
                "{\"project_id\":555,\"error_list\":" + errorList + "}");
    }

    protected void stubProjectStatus(String successIds, String errors) {
        stubPost("/open_api/v3.0/project/status/update/",
                "{\"project_ids\":" + successIds + ",\"errors\":" + errors + "}");
    }

    protected void stubMaterialStatus(String successIds, String errors) {
        stubPost("/open_api/v3.0/oc_project/material/status/update/",
                "{\"project_id\":555,\"material_ids\":" + successIds + ",\"errors\":" + errors + "}");
    }

    protected void stubMonetization(String successIds, String errors) {
        stubPost("/open_api/v3.0/uni_project/monetization_mode/update/",
                "{\"project_ids\":" + successIds + ",\"errors\":" + errors + "}");
    }

    protected void stubProjectList(String listJson) {
        stubGet("/open_api/v3.0/uni_project_list/get/",
                "{\"list\":" + listJson + ",\"page_info\":{\"total_page\":1}}");
    }

    protected Map<String, Object> body(Object value) {
        return JSON.convertValue(value, JSON.getTypeFactory()
                .constructMapType(java.util.LinkedHashMap.class, String.class, Object.class));
    }
}
