package com.unidelivery.app.service;

import com.unidelivery.app.dto.PreCheckItem;

import java.util.ArrayList;
import java.util.List;

/** 预检未全绿：禁止创建，fail 项带原因与证据。 */
public class PreCheckFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<PreCheckItem> failures;

    public PreCheckFailedException(List<PreCheckItem> failures) {
        super("预检未通过: " + failures.stream().map(PreCheckItem::item).toList());
        this.failures = new ArrayList<>(failures);
    }

    public List<PreCheckItem> failures() {
        return List.copyOf(failures);
    }
}
