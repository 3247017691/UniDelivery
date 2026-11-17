package com.unidelivery.app.config;

import com.unidelivery.app.service.AuditService;
import com.unidelivery.app.service.TokenService;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Configuration
public class MapiClientConfig {

    @Bean
    public MapiConfig mapiConfig(MapiProperties properties) {
        return new MapiConfig(
                properties.baseUrl(),
                Duration.ofMillis(properties.connectTimeoutMs()),
                Duration.ofMillis(properties.requestTimeoutMs()),
                properties.perAdvertiserQps(),
                properties.globalQps(),
                properties.maxRetries(),
                Duration.ofMillis(properties.retryBackoffMs()),
                Set.copyOf(properties.retryableCodes()),
                properties.paths());
    }

    @Bean
    public MapiClient mapiClient(MapiConfig config, TokenService tokenService, AuditService auditService) {
        return new MapiClient(config, tokenService, auditService);
    }
}
