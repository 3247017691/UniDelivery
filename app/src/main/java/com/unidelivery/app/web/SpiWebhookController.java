package com.unidelivery.app.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.service.SubscriptionService;
import com.unidelivery.app.spi.SpiSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SPI 回调（契约核实：同时支持 GET/POST；X-Open-Signature 验签；
 * challenge 验证应答 {BaseResp:{StatusCode,StatusMessage},challenge}；
 * 200 成功 / 4xx 不重试 / 5xx 重试）。验签失败=400，处理异常=500 交由平台重试。
 */
@RestController
@RequestMapping("/spi")
public class SpiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SpiWebhookController.class);

    private final Map<String, SpiSignatureVerifier> verifiers;
    private final UniDeliveryProperties properties;
    private final SubscriptionService subscriptions;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpiWebhookController(Map<String, SpiSignatureVerifier> verifiers, UniDeliveryProperties properties,
                                SubscriptionService subscriptions) {
        this.verifiers = verifiers;
        this.properties = properties;
        this.subscriptions = subscriptions;
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> challengeGet(
            @RequestParam(name = "challenge", required = false) Long challenge,
            @RequestParam(name = "event", required = false) String event) {
        if ("verify_webhook".equals(event)) {
            return ok(challenge);
        }
        return baseResp(400, "GET 仅用于 challenge 验证");
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestBody byte[] rawBody,
            @RequestHeader(name = "X-Open-Signature", required = false) String signature) {
        JsonNode envelope;
        try {
            envelope = mapper.readTree(rawBody);
        } catch (Exception e) {
            return baseResp(400, "body 非 JSON");
        }
        if ("verify_webhook".equals(envelope.path("event").asText())) {
            return ok(envelope.path("challenge").asLong());
        }
        SpiSignatureVerifier verifier = verifiers.getOrDefault(properties.spiVerifier(),
                verifiers.get("rejectAllSpiVerifier"));
        if (verifier == null || !verifier.verify(rawBody, signature)) {
            log.warn("SPI 验签失败 message_id={}", envelope.path("message_id").asText());
            return baseResp(400, "signature invalid");
        }
        try {
            SubscriptionService.ReceiveResult result = subscriptions.receive(envelope);
            return baseResp(200, result.duplicate() ? "duplicate" : "ok");
        } catch (IllegalArgumentException e) {
            return baseResp(400, e.getMessage());
        } catch (RuntimeException e) {
            log.error("SPI 处理异常（返回 5xx 交平台重试）: {}", e.getMessage());
            return baseResp(500, "internal error");
        }
    }

    private ResponseEntity<Map<String, Object>> ok(Long challenge) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("BaseResp", Map.of("StatusCode", 200, "StatusMessage", "success"));
        body.put("challenge", challenge);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> baseResp(int statusCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("BaseResp", Map.of("StatusCode", statusCode, "StatusMessage", message));
        return ResponseEntity.status(statusCode).body(body);
    }
}
