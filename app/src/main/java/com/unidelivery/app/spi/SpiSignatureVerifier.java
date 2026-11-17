package com.unidelivery.app.spi;

/** SPI 回调验签出口；实现 bean 名由 unidelivery.spi-verifier 选择。 */
public interface SpiSignatureVerifier {

    boolean verify(byte[] rawBody, String signatureHeader);
}
