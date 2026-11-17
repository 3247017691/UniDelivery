package com.unidelivery.app.spi;

import com.unidelivery.app.config.UniDeliveryProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 官方验签算法（契约核实，三语言示例一致）：HMAC-SHA256(secret_key, 回调 body 原始字节)，
 * 小写 hex，与 X-Open-Signature 字符串相等比较；无字段拼接。secret 未配置即拒绝。
 */
@Component("hmacSha256SpiVerifier")
public class HmacSha256SpiVerifier implements SpiSignatureVerifier {

    private final UniDeliveryProperties properties;

    public HmacSha256SpiVerifier(UniDeliveryProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean verify(byte[] rawBody, String signatureHeader) {
        String secret = properties.spiSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawBody));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            return false;
        }
    }
}
