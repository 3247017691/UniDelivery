package com.unidelivery.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "mapi")
public record MapiProperties(
        @DefaultValue("https://api.oceanengine.com") String baseUrl,
        @DefaultValue("5000") long connectTimeoutMs,
        @DefaultValue("30000") long requestTimeoutMs,
        @DefaultValue("5") double perAdvertiserQps,
        @DefaultValue("20") double globalQps,
        @DefaultValue("3") int maxRetries,
        @DefaultValue("500") long retryBackoffMs,
        @DefaultValue("40100") List<Integer> retryableCodes,
        @DefaultValue Map<String, String> paths) {
}
