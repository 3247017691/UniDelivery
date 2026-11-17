package com.unidelivery.app.service;

import com.unidelivery.app.config.UniDeliveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 告警出口：结构化日志必达；配置 webhook 时附加推送（失败不阻断主流程）。
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final UniDeliveryProperties properties;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AlertService(UniDeliveryProperties properties) {
        this.properties = properties;
    }

    public void alert(String level, String title, String detail) {
        log.warn("ALERT[{}] {} :: {}", level, title, detail);
        String url = properties.alertWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String body = mapper.writeValueAsString(Map.of("level", level, "title", title, "detail", detail));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("告警 webhook 返回 http={}", response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("告警 webhook 推送失败: {}", e.getMessage());
        }
    }
}
