package com.unidelivery.app.spi;

import org.springframework.stereotype.Component;

/** 默认拒绝一切推送：验签实现未选定/secret 未配置时绝不裸收。 */
@Component("rejectAllSpiVerifier")
public class RejectAllSpiVerifier implements SpiSignatureVerifier {

    @Override
    public boolean verify(byte[] rawBody, String signatureHeader) {
        return false;
    }
}
