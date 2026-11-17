package com.unidelivery.mapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MAPI 统一客户端：限流（全局 + 账户级令牌桶）、指数退避重试、封套校验（code==0 才成功）、
 * 逐次审计。幂等是业务层职责（journal + 先查后重试），本类不隐式重发写请求以外的语义。
 */
public final class MapiClient {

    private static final Logger log = LoggerFactory.getLogger(MapiClient.class);
    private static final int SUMMARY_LIMIT = 500;

    private final MapiConfig config;
    private final MapiPaths paths;
    private final HttpTransport transport;
    private final AccessTokenProvider tokenProvider;
    private final AuditSink auditSink;
    private final ObjectMapper mapper;
    private final TokenBucket globalBucket;
    private final ConcurrentHashMap<Long, TokenBucket> advertiserBuckets = new ConcurrentHashMap<>();

    public MapiClient(MapiConfig config, AccessTokenProvider tokenProvider, AuditSink auditSink) {
        this.config = config;
        this.paths = new MapiPaths(config.pathOverrides());
        this.transport = new HttpTransport(config.connectTimeout(), config.requestTimeout());
        this.tokenProvider = tokenProvider;
        this.auditSink = auditSink;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.globalBucket = new TokenBucket(config.globalQps(), config.globalQps());
    }

    public MapiResult call(MapiEndpoint endpoint, long advertiserId, Map<String, Object> params) {
        return execute(endpoint, advertiserId, params, null, null, null);
    }

    public <T> T call(MapiEndpoint endpoint, long advertiserId, Map<String, Object> params, Class<T> type) {
        MapiResult result = call(endpoint, advertiserId, params);
        return convert(result, type);
    }

    public <T> T call(MapiEndpoint endpoint, long advertiserId, Map<String, Object> params,
                      TypeReference<T> type) {
        MapiResult result = call(endpoint, advertiserId, params);
        return convert(result, type);
    }

    /** 同步视频上传（multipart）。文件 MD5 去重由业务层负责。 */
    public MapiResult uploadVideo(long advertiserId, Map<String, String> fields, Path file, String filename) {
        return execute(MapiEndpoint.VIDEO_UPLOAD, advertiserId, Map.of(), fields, file, filename);
    }

    public String resolvePath(MapiEndpoint endpoint) {
        return paths.resolve(endpoint);
    }

    private <T> T convert(MapiResult result, Class<T> type) {
        JsonNode data = result.data();
        if (data == null || data.isNull()) {
            return null;
        }
        return mapper.convertValue(data, type);
    }

    private <T> T convert(MapiResult result, TypeReference<T> type) {
        JsonNode data = result.data();
        if (data == null || data.isNull()) {
            return null;
        }
        return mapper.convertValue(data, type);
    }

    private MapiResult execute(MapiEndpoint endpoint, long advertiserId, Map<String, Object> params,
                               Map<String, String> multipartFields, Path file, String filename) {
        String path = paths.resolve(endpoint);
        String host = endpoint.host() != null ? endpoint.host() : config.baseUrl();
        String url = host + path;
        String requestSummary = summarize(endpoint, params, multipartFields);
        int attempt = 0;
        while (true) {
            globalBucket.acquire();
            advertiserBuckets.computeIfAbsent(advertiserId,
                    id -> new TokenBucket(config.perAdvertiserQps(), config.perAdvertiserQps())).acquire();
            long start = System.nanoTime();
            int httpStatus = -1;
            int code = -1;
            String message = "";
            String requestId = "";
            JsonNode data = null;
            boolean transportError = false;
            try {
                String token = tokenProvider.token(advertiserId);
                HttpTransport.RawResponse response = switch (endpoint.method()) {
                    case GET -> transport.get(url + query(params), token);
                    case POST -> transport.postJson(url, token, mapper.writeValueAsString(params));
                    case MULTIPART -> transport.postMultipart(url, token, multipartFields,
                            "video_file", file, filename);
                };
                httpStatus = response.status();
                JsonNode envelope = parseEnvelope(response.body());
                if (envelope == null) {
                    transportError = true;
                    message = "非 JSON 应答（网关/代理拦截），http=" + httpStatus;
                } else {
                    code = envelope.path("code").asInt(-1);
                    message = envelope.path("message").asText("");
                    requestId = envelope.path("request_id").asText("");
                    data = envelope.get("data");
                }
            } catch (IOException e) {
                transportError = true;
                message = "传输错误: " + e.getMessage();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MapiException(endpoint, -1, "调用被中断", "", true);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            boolean ok = !transportError && httpStatus == 200 && code == 0;
            boolean retryable = transportError || httpStatus == 429 || httpStatus >= 500
                    || config.retryableCodes().contains(code) || code == 40102;
            if (code == 40102) {
                tokenProvider.invalidate(advertiserId);
            }
            if (ok) {
                audit(endpoint, path, advertiserId, requestSummary, httpStatus, code, requestId,
                        elapsedMs, attempt, "OK");
                return new MapiResult(code, message, requestId, data);
            }
            boolean willRetry = retryable && attempt < config.maxRetries();
            audit(endpoint, path, advertiserId, requestSummary, httpStatus, code, requestId,
                    elapsedMs, attempt, willRetry ? "RETRY" : (transportError ? "TRANSPORT_ERROR" : "BIZ_ERROR"));
            if (!willRetry) {
                throw new MapiException(endpoint, code, message, requestId, retryable);
            }
            log.warn("MAPI {} 第 {} 次尝试失败(code={}, http={})，退避重试: {}",
                    endpoint.name(), attempt + 1, code, httpStatus, message);
            sleepBackoff(attempt);
            attempt++;
        }
    }

    private JsonNode parseEnvelope(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(body);
            return node.isObject() && node.has("code") ? node : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String query(Map<String, Object> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&", "?", "");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String text = value instanceof String s ? s : writeJson(value);
            joiner.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
        }
        return joiner.toString();
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalArgumentException("参数序列化失败: " + e.getMessage(), e);
        }
    }

    private String summarize(MapiEndpoint endpoint, Map<String, Object> params, Map<String, String> fields) {
        String raw = endpoint.method() == MapiEndpoint.Http.MULTIPART
                ? String.valueOf(fields)
                : writeJson(params);
        return raw.length() <= SUMMARY_LIMIT ? raw : raw.substring(0, SUMMARY_LIMIT) + "...(truncated)";
    }

    private void audit(MapiEndpoint endpoint, String path, long advertiserId, String summary,
                       int httpStatus, int code, String requestId, long elapsedMs, int attempt, String outcome) {
        try {
            auditSink.audit(new AuditSink.AuditEvent(endpoint, path, advertiserId, summary,
                    httpStatus, code, requestId, elapsedMs, attempt, outcome));
        } catch (RuntimeException e) {
            log.error("审计写入失败（不阻断主流程）: {}", e.getMessage());
        }
    }

    private void sleepBackoff(int attempt) {
        long millis = config.retryBackoff().toMillis() * (1L << Math.min(attempt, 6));
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("重试退避被中断", e);
        }
    }
}
