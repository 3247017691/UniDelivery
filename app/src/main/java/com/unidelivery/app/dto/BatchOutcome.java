package com.unidelivery.app.dto;

import java.util.List;

/** 批量接口「成功集合 + errors 明细」的部分成功语义；errors 非空即不算全成功。 */
public record BatchOutcome(List<Long> successIds, List<ErrorEntry> errors) {

    public record ErrorEntry(Long id, String message) {
    }

    @com.fasterxml.jackson.annotation.JsonProperty("fullSuccess")
    public boolean fullSuccess() {
        return errors.isEmpty();
    }
}
