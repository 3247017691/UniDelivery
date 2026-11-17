package com.unidelivery.app.service;

/** 唯一标的冲突：同一 aweme_id x asset_id 已存在活跃全域项目。 */
public class TargetConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TargetConflictException(String message) {
        super(message);
    }
}
