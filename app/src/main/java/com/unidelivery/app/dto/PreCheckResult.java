package com.unidelivery.app.dto;

import java.util.List;

public record PreCheckResult(List<PreCheckItem> items) {

    @com.fasterxml.jackson.annotation.JsonProperty("allPassed")
    public boolean allPassed() {
        return items.stream().allMatch(PreCheckItem::passed);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("failures")
    public List<PreCheckItem> failures() {
        return items.stream().filter(i -> !i.passed()).toList();
    }
}
