package com.unidelivery.app.service;

/** 需要业务显式二次确认（创建全域将停止同标的在投标准项目）。 */
public class ConfirmationRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfirmationRequiredException(String message) {
        super(message);
    }
}
