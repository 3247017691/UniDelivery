package com.unidelivery.mapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 通用应答封套：code/message/data/request_id。只有 code==0 算成功。
 */
public record MapiResult(int code, String message, String requestId, JsonNode data) {

    public boolean ok() {
        return code == 0;
    }
}
