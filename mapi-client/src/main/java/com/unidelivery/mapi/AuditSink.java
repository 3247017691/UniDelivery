package com.unidelivery.mapi;

/**
 * 请求审计出口：每次尝试（含重试）都记录。app 模块实现为日志 + 写操作 journal。
 */
@FunctionalInterface
public interface AuditSink {

    void audit(AuditEvent event);

    /**
     * @param outcome OK / BIZ_ERROR / RETRY / TRANSPORT_ERROR
     */
    record AuditEvent(
            MapiEndpoint endpoint,
            String path,
            long advertiserId,
            String requestSummary,
            int httpStatus,
            int code,
            String requestId,
            long elapsedMs,
            int attempt,
            String outcome) {
    }
}
