package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.MapiProperties;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.mapi.HttpTransport;
import com.unidelivery.mapi.MapiEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * SPI 补拉（契约核实：/open_api/2/spi_task/get/，host ad.oceanengine.com，APP-Access-Token，
 * 仅 2 天内数据）：把推送失败/漏收的消息重新走 receive 去重消费，兜底 at-least-once。
 */
@Service
public class SpiBackfillService {

    private static final Logger log = LoggerFactory.getLogger(SpiBackfillService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGES = 20;

    private final UniDeliveryProperties properties;
    private final TokenService tokens;
    private final SubscriptionService subscriptions;
    private final HttpTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpiBackfillService(UniDeliveryProperties properties, MapiProperties mapiProperties,
                              TokenService tokens, SubscriptionService subscriptions) {
        this.properties = properties;
        this.tokens = tokens;
        this.subscriptions = subscriptions;
        this.transport = new HttpTransport(Duration.ofMillis(mapiProperties.connectTimeoutMs()),
                Duration.ofMillis(mapiProperties.requestTimeoutMs()));
    }

    public int backfill() {
        if (properties.spi().subscribeTaskIds().isEmpty()) {
            return 0;
        }
        int recovered = 0;
        String token = tokens.appAccessToken();
        for (Long taskId : properties.spi().subscribeTaskIds()) {
            long cursor = 0;
            for (int page = 0; page < MAX_PAGES; page++) {
                StringJoiner query = new StringJoiner("&", "?", "");
                query.add("app_id=" + enc(properties.appId()));
                query.add("subscribe_id=" + taskId);
                query.add("start_date=" + enc(LocalDateTime.now().minusHours(1).format(TS)));
                query.add("end_date=" + enc(LocalDateTime.now().minusMinutes(30).format(TS)));
                query.add("status=ALL");
                query.add("cursor=" + cursor);
                query.add("count=100");
                String url = MapiEndpoint.SPI_TASK_GET.host() + MapiEndpoint.SPI_TASK_GET.defaultPath() + query;
                try {
                    HttpTransport.RawResponse response =
                            transport.getWithHeader(url, "APP-Access-Token", token);
                    JsonNode envelope = mapper.readTree(response.body());
                    if (envelope.path("code").asInt(-1) != 0) {
                        log.error("SPI 补拉失败 task={} code={}", taskId, envelope.path("code").asInt());
                        break;
                    }
                    for (JsonNode item : envelope.path("data").path("list")) {
                        if (!"SUCCESS".equals(item.path("status").asText())
                                && !"FAIL".equals(item.path("status").asText())) {
                            continue;
                        }
                        JsonNode payload = mapper.readTree(item.path("data").asText());
                        SubscriptionService.ReceiveResult result = subscriptions.receive(payload);
                        if (!result.duplicate()) {
                            recovered++;
                        }
                    }
                    JsonNode cursorInfo = envelope.path("data").path("cursor_info");
                    if (!cursorInfo.path("has_more").asBoolean(false)) {
                        break;
                    }
                    cursor = cursorInfo.path("cursor").asLong();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return recovered;
                } catch (Exception e) {
                    log.error("SPI 补拉异常 task={}: {}", taskId, e.getMessage());
                    break;
                }
            }
        }
        return recovered;
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
