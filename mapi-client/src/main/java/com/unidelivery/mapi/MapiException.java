package com.unidelivery.mapi;

/**
 * MAPI 业务/传输错误。code=0 才成功；其余一律异常，绝不把失败当空结果。
 * code=-1 表示传输层错误（超时/IO），retryable=true。
 */
public class MapiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String requestId;
    private final boolean retryable;
    private final MapiEndpoint endpoint;

    public MapiException(MapiEndpoint endpoint, int code, String message, String requestId, boolean retryable) {
        super("MAPI " + endpoint.name() + " code=" + code + " message=" + message + " request_id=" + requestId);
        this.endpoint = endpoint;
        this.code = code;
        this.requestId = requestId;
        this.retryable = retryable;
    }

    public int code() {
        return code;
    }

    public String requestId() {
        return requestId;
    }

    public boolean retryable() {
        return retryable;
    }

    public MapiEndpoint endpoint() {
        return endpoint;
    }
}
