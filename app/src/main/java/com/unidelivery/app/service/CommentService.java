package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 评论管理（兼容全域项目）：读接口直连；写接口（回复/隐藏）走 journal 审计。
 * 字段契约未逐页核实 → 参数透传，由操作者按官方文档组装。
 */
@Service
public class CommentService {

    private final MapiClient mapi;
    private final DeliveryService delivery;

    public CommentService(MapiClient mapi, DeliveryService delivery) {
        this.mapi = mapi;
        this.delivery = delivery;
    }

    public JsonNode list(long advertiserId, Map<String, Object> params) {
        return mapi.call(MapiEndpoint.COMMENT_GET, advertiserId, withAdvertiser(advertiserId, params),
                JsonNode.class);
    }

    public JsonNode replies(long advertiserId, Map<String, Object> params) {
        return mapi.call(MapiEndpoint.COMMENT_REPLY_GET, advertiserId, withAdvertiser(advertiserId, params),
                JsonNode.class);
    }

    public JsonNode metrics(long advertiserId, Map<String, Object> params) {
        return mapi.call(MapiEndpoint.COMMENT_METRICS, advertiserId, withAdvertiser(advertiserId, params),
                JsonNode.class);
    }

    public JsonNode mid2itemId(long advertiserId, Map<String, Object> params) {
        return mapi.call(MapiEndpoint.COMMENT_MID2ITEM, advertiserId, withAdvertiser(advertiserId, params),
                JsonNode.class);
    }

    public JsonNode reply(long advertiserId, Map<String, Object> payload, String clientRequestId) {
        return delivery.auditedPassthrough(MapiEndpoint.COMMENT_REPLY, advertiserId,
                withAdvertiser(advertiserId, payload),
                clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId, null);
    }

    public JsonNode hide(long advertiserId, Map<String, Object> payload, String clientRequestId) {
        return delivery.auditedPassthrough(MapiEndpoint.COMMENT_HIDE, advertiserId,
                withAdvertiser(advertiserId, payload),
                clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId, null);
    }

    private Map<String, Object> withAdvertiser(long advertiserId, Map<String, Object> params) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>(params);
        merged.put("advertiser_id", advertiserId);
        return merged;
    }
}
