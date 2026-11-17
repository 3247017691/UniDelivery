package com.unidelivery.app.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.repo.ReportSnapshotRepository;
import com.unidelivery.app.repo.SubscriptionEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SPI 回调契约：challenge、HMAC-SHA256 验签、message_id 去重、数据就绪→实时拉取、口径缺失不丢事件。 */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:e2e_spi;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class SpiWebhookTest extends MapiStubBase {

    @Autowired
    private SubscriptionEventRepository events;
    @Autowired
    private ReportSnapshotRepository snapshots;

    private void stubReportRows() {
        stubGet("/open_api/v3.0/report/custom/get/",
                "{\"rows\":[{\"dimensions\":{\"project_id\":\"555\",\"stat_time\":\"2026-11-17 10:00:00\"},"
                        + "\"metrics\":{\"stat_cost\":\"12.5\"}}],\"total_metrics\":{},"
                        + "\"page_info\":{\"total_page\":1}}");
    }

    private HttpEntity<byte[]> signed(Object envelope) throws Exception {
        byte[] body = JSON.writeValueAsBytes(envelope);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-spi-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Open-Signature", HexFormat.of().formatHex(mac.doFinal(body)));
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> envelope(String messageId, String serviceLabel) {
        return Map.of(
                "message_id", messageId,
                "subscribe_task_id", 1,
                "advertiser_ids", List.of(1001),
                "publish_time", System.currentTimeMillis(),
                "timestamp", System.currentTimeMillis(),
                "nonce", 123,
                "service_label", serviceLabel,
                "data", Map.of("user_id", 1001, "event", "PRODUCT",
                        "content", Map.of("project_ids", List.of(555))));
    }

    @Test
    void challengeVerificationEchoes() {
        ResponseEntity<JsonNode> response = rest.getForEntity(
                "/spi/callback?challenge=42&event=verify_webhook", JsonNode.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(42, response.getBody().path("challenge").asLong());
        assertEquals(200, response.getBody().path("BaseResp").path("StatusCode").asInt());
    }

    @Test
    void invalidSignatureIs400NoRetry() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Open-Signature", "deadbeef");
        ResponseEntity<JsonNode> response = rest.exchange("/spi/callback", HttpMethod.POST,
                new HttpEntity<>(JSON.writeValueAsBytes(envelope("m-bad", "report.uniproject.activeprogram")),
                        headers), JsonNode.class);
        assertEquals(400, response.getStatusCode().value());
        assertEquals(0, events.findByEventId("m-bad").isPresent() ? 1 : 0);
    }

    @Test
    void validPushConsumesOnceAndDedups() throws Exception {
        seedAdvertiser(1001);
        stubReportRows();
        ResponseEntity<JsonNode> first = rest.exchange("/spi/callback", HttpMethod.POST,
                signed(envelope("m-1", "report.uniproject.activeprogram")), JsonNode.class);
        assertEquals(200, first.getStatusCode().value());
        assertTrue(events.findByEventId("m-1").orElseThrow().consumed);
        assertEquals(1, snapshots.findByAdvertiserIdAndTopicAndChannelAndWindowStartBetween(
                1001L, "UNI_PROJECT_DATA", "REALTIME",
                LocalDateTime.of(2026, 11, 17, 0, 0), LocalDateTime.of(2026, 11, 18, 0, 0)).size());
        int calls = MAPI.findAll(getRequestedFor(urlPathEqualTo("/open_api/v3.0/report/custom/get/"))).size();
        assertEquals(1, calls);

        ResponseEntity<JsonNode> duplicate = rest.exchange("/spi/callback", HttpMethod.POST,
                signed(envelope("m-1", "report.uniproject.activeprogram")), JsonNode.class);
        assertEquals(200, duplicate.getStatusCode().value());
        assertEquals(1, events.findByConsumedFalse().stream().filter(e -> e.eventId.equals("m-1")).count()
                + events.findAll().stream().filter(e -> e.eventId.equals("m-1")).count());
        assertEquals(calls, MAPI.findAll(getRequestedFor(
                urlPathEqualTo("/open_api/v3.0/report/custom/get/"))).size());
    }

    @Test
    void missingReportSpecKeepsEventPendingNotLost() throws Exception {
        ResponseEntity<JsonNode> response = rest.exchange("/spi/callback", HttpMethod.POST,
                signed(envelope("m-2", "report.unimaterial.activeprogram")), JsonNode.class);
        assertEquals(200, response.getStatusCode().value());
        assertFalse(events.findByEventId("m-2").orElseThrow().consumed);
    }
}
