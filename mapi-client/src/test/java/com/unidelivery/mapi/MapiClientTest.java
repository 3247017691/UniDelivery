package com.unidelivery.mapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SDK 契约测试：封套校验、限流重试、token 失效续期、审计、path 覆盖、multipart。 */
class MapiClientTest {

    private WireMockServer server;
    private MapiClient client;
    private List<AuditSink.AuditEvent> audits;
    private AtomicInteger invalidations;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        audits = new ArrayList<>();
        invalidations = new AtomicInteger();
        AccessTokenProvider provider = new AccessTokenProvider() {
            @Override
            public String token(long advertiserId) {
                return "token-" + advertiserId;
            }

            @Override
            public void invalidate(long advertiserId) {
                invalidations.incrementAndGet();
            }
        };
        MapiConfig config = new MapiConfig(server.baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(5),
                1000, 1000, 2, Duration.ofMillis(10), Set.of(40100, 40110, 40130, 50000), Map.of());
        client = new MapiClient(config, provider, audits::add);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private void stubPost(String path, String body) {
        server.stubFor(post(urlPathEqualTo(path)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody(body)));
    }

    @Test
    void successEnvelopeAndTypedData() {
        stubPost("/open_api/v3.0/uni_project/create/",
                "{\"code\":0,\"message\":\"OK\",\"data\":{\"project_id\":555},\"request_id\":\"r1\"}");
        JsonNode data = client.call(MapiEndpoint.UNI_PROJECT_CREATE, 1001,
                Map.of("advertiser_id", 1001), JsonNode.class);
        assertEquals(555, data.path("project_id").asLong());
        assertEquals(1, audits.size());
        assertEquals("OK", audits.get(0).outcome());
        server.verify(postRequestedFor(urlPathEqualTo("/open_api/v3.0/uni_project/create/"))
                .withHeader("Access-Token", equalTo("token-1001")));
    }

    @Test
    void businessErrorIsNotRetriedAndNotOk() {
        stubPost("/open_api/v3.0/uni_project/create/",
                "{\"code\":40000,\"message\":\"name: value is required but missing\",\"request_id\":\"r2\"}");
        MapiException e = assertThrows(MapiException.class, () ->
                client.call(MapiEndpoint.UNI_PROJECT_CREATE, 1001, Map.of()));
        assertEquals(40000, e.code());
        assertEquals("r2", e.requestId());
        server.verify(1, postRequestedFor(urlPathEqualTo("/open_api/v3.0/uni_project/create/")));
        assertEquals("BIZ_ERROR", audits.get(0).outcome());
    }

    @Test
    void rateLimitCodeRetriesWithBackoffThenSucceeds() {
        String path = "/open_api/v3.0/uni_project_list/get/";
        server.stubFor(get(urlPathEqualTo(path)).inScenario("rl")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withBody("{\"code\":40100,\"message\":\"Too many requests.\"}"))
                .willSetStateTo("second"));
        server.stubFor(get(urlPathEqualTo(path)).inScenario("rl")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withBody("{\"code\":0,\"data\":{\"list\":[]},\"request_id\":\"r3\"}")));
        MapiResult result = client.call(MapiEndpoint.UNI_PROJECT_LIST, 1001, Map.of("advertiser_id", 1001));
        assertTrue(result.ok());
        server.verify(2, getRequestedFor(urlPathEqualTo(path)));
        assertEquals(List.of("RETRY", "OK"), audits.stream().map(AuditSink.AuditEvent::outcome).toList());
    }

    @Test
    void expiredTokenInvalidatesAndRetries() {
        String path = "/open_api/v3.0/uni_project/create/";
        server.stubFor(post(urlPathEqualTo(path)).inScenario("tok")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withBody("{\"code\":40102,\"message\":\"access_token已过期\"}"))
                .willSetStateTo("refreshed"));
        server.stubFor(post(urlPathEqualTo(path)).inScenario("tok")
                .whenScenarioStateIs("refreshed")
                .willReturn(aResponse().withBody("{\"code\":0,\"data\":{\"project_id\":1}}")));
        client.call(MapiEndpoint.UNI_PROJECT_CREATE, 1001, Map.of());
        assertEquals(1, invalidations.get());
        server.verify(2, postRequestedFor(urlPathEqualTo(path)));
    }

    @Test
    void nonJsonGatewayBodyIsTransportError() {
        stubPost("/open_api/v3.0/uni_project/create/", "<html>gateway</html>");
        MapiException e = assertThrows(MapiException.class, () ->
                client.call(MapiEndpoint.UNI_PROJECT_CREATE, 1001, Map.of()));
        assertEquals(-1, e.code());
        assertTrue(e.retryable());
        server.verify(3, postRequestedFor(urlPathEqualTo("/open_api/v3.0/uni_project/create/")));
    }

    @Test
    void getQueryEncodesComplexFiltering() {
        String path = "/open_api/v3.0/oc_project/material/get/";
        server.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse()
                .withBody("{\"code\":0,\"data\":{\"video_material_list\":[]}}")));
        client.call(MapiEndpoint.OC_MATERIAL_GET, 1001,
                Map.of("advertiser_id", 1001, "filtering", Map.of("material_type", "VIDEO")));
        server.verify(getRequestedFor(urlPathEqualTo(path))
                .withQueryParam("advertiser_id", equalTo("1001"))
                .withQueryParam("filtering", containing("material_type")));
    }

    @Test
    void pathOverrideFromConfig() {
        MapiConfig config = new MapiConfig(server.baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(5),
                1000, 1000, 0, Duration.ofMillis(10), Set.of(),
                Map.of("UNI_PROJECT_LIST", "/open_api/v3.0/uni_project/list/legacy/"));
        MapiClient overridden = new MapiClient(config, adv -> "t", audits::add);
        server.stubFor(get(urlPathEqualTo("/open_api/v3.0/uni_project/list/legacy/"))
                .willReturn(aResponse().withBody("{\"code\":0,\"data\":{}}")));
        assertTrue(overridden.call(MapiEndpoint.UNI_PROJECT_LIST, 1001, Map.of()).ok());
    }

    @Test
    void multipartUploadCarriesSignatureAndFile() throws Exception {
        Path file = Files.createTempFile("mapi-test-", ".mp4");
        Files.write(file, new byte[]{1, 2, 3, 4});
        server.stubFor(post(urlPathEqualTo("/open_api/2/file/video/ad/"))
                .willReturn(aResponse().withBody("{\"code\":0,\"data\":{\"video_id\":\"v1\"}}")));
        MapiResult result = client.uploadVideo(1001,
                Map.of("advertiser_id", "1001", "upload_type", "UPLOAD_BY_FILE", "video_signature", "md5x"),
                file, "a.mp4");
        assertEquals("v1", result.data().path("video_id").asText());
        server.verify(postRequestedFor(urlPathEqualTo("/open_api/2/file/video/ad/"))
                .withRequestBody(containing("video_signature"))
                .withRequestBody(containing("md5x"))
                .withRequestBody(containing("a.mp4")));
        Files.deleteIfExists(file);
    }
}
