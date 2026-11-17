package com.unidelivery.mapi;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * SDK 配置。retryableCodes 为平台限流/瞬时错误码集合，以契约核实结果为准（配置可改）。
 */
public record MapiConfig(
        String baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        double perAdvertiserQps,
        double globalQps,
        int maxRetries,
        Duration retryBackoff,
        Set<Integer> retryableCodes,
        Map<String, String> pathOverrides) {

    public static MapiConfig defaults() {
        return new MapiConfig(
                "https://api.oceanengine.com",
                Duration.ofSeconds(5),
                Duration.ofSeconds(30),
                5.0,
                20.0,
                3,
                Duration.ofMillis(500),
                Set.of(40100, 40110, 40130, 50000),
                Map.of());
    }

    public MapiConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries 不能为负");
        }
        retryableCodes = Set.copyOf(retryableCodes);
        pathOverrides = Map.copyOf(pathOverrides);
    }
}
