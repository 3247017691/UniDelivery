package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.MapiProperties;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.OauthToken;
import com.unidelivery.app.repo.OauthTokenRepository;
import com.unidelivery.mapi.AccessTokenProvider;
import com.unidelivery.mapi.HttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 token 生命周期：授权码换 token、定时续期、按账户提供 access_token。
 * OAuth 端点为开放平台稳定公开路径；不走 MapiClient（避免 token 提供递归）。
 */
@Service
public class TokenService implements AccessTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String OAUTH_ACCESS_TOKEN = "/open_api/oauth2/" + "access_token/";
    private static final String OAUTH_REFRESH_TOKEN = "/open_api/oauth2/" + "refresh_token/";
    private static final String OAUTH_APP_ACCESS_TOKEN = "/open_api/oauth2/" + "app_access_token/";
    private static final String AUTHORIZE_PAGE = "https://open.oceanengine.com/audit/oauth.html";
    private static final Duration REFRESH_AHEAD = Duration.ofMinutes(10);

    private final OauthTokenRepository repository;
    private final UniDeliveryProperties properties;
    private final HttpTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public TokenService(OauthTokenRepository repository, UniDeliveryProperties properties,
                        MapiProperties mapiProperties) {
        this.repository = repository;
        this.properties = properties;
        this.baseUrl = mapiProperties.baseUrl();
        this.transport = new HttpTransport(
                Duration.ofMillis(mapiProperties.connectTimeoutMs()),
                Duration.ofMillis(mapiProperties.requestTimeoutMs()));
    }

    public String authorizeUrl(String redirectUri, String state) {
        return AUTHORIZE_PAGE + "?app_id=" + properties.appId()
                + "&state=" + state + "&redirect_uri=" + redirectUri;
    }

    @Transactional
    public OauthToken exchangeAuthCode(long advertiserId, String authCode) {
        JsonNode data = oauthCall(OAUTH_ACCESS_TOKEN, Map.of(
                "app_id", properties.appId(),
                "secret", properties.appSecret(),
                "grant_type", "auth_code",
                "auth_code", authCode));
        return store(advertiserId, data);
    }

    @Override
    public String token(long advertiserId) {
        OauthToken row = repository.findById(advertiserId)
                .orElseThrow(() -> new IllegalStateException("账户 " + advertiserId + " 未完成 OAuth 授权"));
        if (row.expiresAt.isBefore(LocalDateTime.now().plusMinutes(5))) {
            return refresh(row).accessToken;
        }
        return row.accessToken;
    }

    @Override
    @Transactional
    public void invalidate(long advertiserId) {
        repository.findById(advertiserId).ifPresent(row -> {
            row.expiresAt = LocalDateTime.now();
            row.updatedAt = LocalDateTime.now();
            repository.save(row);
        });
    }

    /** 续期所有临近过期的 token；refresh_token 过期则告警要求重新授权。 */
    @Transactional
    public int refreshExpiring() {
        LocalDateTime horizon = LocalDateTime.now().plus(REFRESH_AHEAD);
        List<OauthToken> due = repository.findAll().stream()
                .filter(t -> t.expiresAt.isBefore(horizon))
                .toList();
        int refreshed = 0;
        for (OauthToken row : due) {
            if (row.refreshExpiresAt.isBefore(LocalDateTime.now())) {
                log.error("账户 {} refresh_token 已过期，需重新走授权流程", row.advertiserId);
                continue;
            }
            refresh(row);
            refreshed++;
        }
        return refreshed;
    }

    private synchronized OauthToken refresh(OauthToken row) {
        OauthToken current = repository.findById(row.advertiserId).orElseThrow();
        if (current.expiresAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            return current;
        }
        JsonNode data = oauthCall(OAUTH_REFRESH_TOKEN, Map.of(
                "app_id", properties.appId(),
                "secret", properties.appSecret(),
                "grant_type", "refresh_token",
                "refresh_token", current.refreshToken));
        return store(current.advertiserId, data);
    }

    private OauthToken store(long advertiserId, JsonNode data) {
        OauthToken row = repository.findById(advertiserId).orElseGet(() -> {
            OauthToken created = new OauthToken();
            created.advertiserId = advertiserId;
            return created;
        });
        row.accessToken = data.path("access_token").asText();
        row.refreshToken = data.path("refresh_token").asText();
        row.expiresAt = LocalDateTime.now().plusSeconds(data.path("expires_in").asLong());
        row.refreshExpiresAt = LocalDateTime.now().plusSeconds(data.path("refresh_token_expires_in").asLong());
        row.updatedAt = LocalDateTime.now();
        return repository.save(row);
    }

    private String appTokenCache;
    private LocalDateTime appTokenExpiresAt = LocalDateTime.MIN;

    /** 应用级 token（SPI 补拉接口要求 APP-Access-Token）。 */
    public synchronized String appAccessToken() {
        if (appTokenCache != null && appTokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            return appTokenCache;
        }
        JsonNode data = oauthCall(OAUTH_APP_ACCESS_TOKEN, Map.of(
                "app_id", properties.appId(),
                "secret", properties.appSecret(),
                "grant_type", "client_credentials"));
        appTokenCache = data.path("app_access_token").asText();
        appTokenExpiresAt = LocalDateTime.now().plusSeconds(data.path("expires_in").asLong());
        return appTokenCache;
    }

    private JsonNode oauthCall(String path, Map<String, String> body) {
        try {
            HttpTransport.RawResponse response = transport.postJson(baseUrl + path, "",
                    mapper.writeValueAsString(body));
            JsonNode envelope = mapper.readTree(response.body());
            if (response.status() != 200 || envelope.path("code").asInt(-1) != 0) {
                throw new IllegalStateException("OAuth 调用失败 http=" + response.status()
                        + " code=" + envelope.path("code").asInt(-1)
                        + " message=" + envelope.path("message").asText(""));
            }
            return envelope.path("data");
        } catch (IOException e) {
            throw new IllegalStateException("OAuth 传输错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OAuth 调用被中断", e);
        }
    }
}
