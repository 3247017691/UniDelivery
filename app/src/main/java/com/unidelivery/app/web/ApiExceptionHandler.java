package com.unidelivery.app.web;

import com.unidelivery.app.service.ConfirmationRequiredException;
import com.unidelivery.app.service.PreCheckFailedException;
import com.unidelivery.app.service.TargetConflictException;
import com.unidelivery.mapi.MapiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** 失败如实上报：预检 422 / 需确认 428 / 标的冲突 409 / 平台错误 502 带 code 与 request_id。 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PreCheckFailedException.class)
    public ResponseEntity<Map<String, Object>> preCheck(PreCheckFailedException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "PRECHECK_FAILED");
        body.put("failures", e.failures());
        return ResponseEntity.status(422).body(body);
    }

    @ExceptionHandler(ConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> confirmation(ConfirmationRequiredException e) {
        return ResponseEntity.status(428).body(Map.of("error", "CONFIRMATION_REQUIRED", "message", e.getMessage()));
    }

    @ExceptionHandler(TargetConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(TargetConflictException e) {
        return ResponseEntity.status(409).body(Map.of("error", "TARGET_CONFLICT", "message", e.getMessage()));
    }

    @ExceptionHandler(MapiException.class)
    public ResponseEntity<Map<String, Object>> mapi(MapiException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "MAPI_ERROR");
        body.put("endpoint", e.endpoint().name());
        body.put("code", e.code());
        body.put("message", e.getMessage());
        body.put("request_id", e.requestId());
        body.put("retryable", e.retryable());
        return ResponseEntity.status(502).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> notReady(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", "NOT_READY", "message", e.getMessage()));
    }

    /** 兜底：未预期异常如实 500 带消息，绝不返回空成功体掩盖故障。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internal(Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", "INTERNAL",
                "message", String.valueOf(e.getMessage())));
    }
}
