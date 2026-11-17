package com.unidelivery.app.dto;

/** 预检单项：pass/fail + 原因 + 证据引用，读不到就是 fail，不退化为空结果即正常。 */
public record PreCheckItem(String item, boolean passed, String reason, String evidence)
        implements java.io.Serializable {

    public static PreCheckItem pass(String item, String reason, String evidence) {
        return new PreCheckItem(item, true, reason, evidence);
    }

    public static PreCheckItem fail(String item, String reason, String evidence) {
        return new PreCheckItem(item, false, reason, evidence);
    }
}
