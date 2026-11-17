package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.ProductPlaylet;
import com.unidelivery.app.repo.ProductPlayletRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 升级版短剧漫剧库商品同步：account_id=升级版组织ID、account_type=EBP（契约核实）。
 * 可投 = status==1 且 audit_status==1（列表页枚举；详情页枚举不一致已记录，以列表页为准）。
 * profession.playlet_id=专辑ID、profession.album_id=剧目ID（API 原文语义）。
 */
@Service
public class ProductSyncService {

    private final MapiClient mapi;
    private final ProductPlayletRepository products;

    public ProductSyncService(MapiClient mapi, ProductPlayletRepository products) {
        this.mapi = mapi;
        this.products = products;
    }

    public record SyncOutcome(boolean ok, String reason) {
    }

    /** 运营录入商品映射（R4：专辑绑定关系本地留痕），可投状态待同步核定。 */
    @Transactional
    public ProductPlaylet record(long advertiserId, long assetId, long orgId, long platformId,
                                 Long playletId, Long albumId) {
        ProductPlaylet row = products.findByAdvertiserIdAndAssetId(advertiserId, assetId)
                .orElseGet(() -> {
                    ProductPlaylet created = new ProductPlaylet();
                    created.advertiserId = advertiserId;
                    created.assetId = assetId;
                    created.deliverable = false;
                    return created;
                });
        row.orgId = orgId;
        row.platformId = platformId;
        if (playletId != null) {
            row.playletId = playletId;
        }
        if (albumId != null) {
            row.albumId = albumId;
        }
        row.libraryVersion = "UPGRADED";
        row.checkedAt = LocalDateTime.now();
        return products.save(row);
    }

    @Transactional
    public SyncOutcome sync(long advertiserId, long assetId) {
        ProductPlaylet row = products.findByAdvertiserIdAndAssetId(advertiserId, assetId).orElse(null);
        if (row == null || row.orgId == null || row.platformId == null) {
            return new SyncOutcome(false, "商品未录入组织ID/商品库ID（record 接口录入后再同步）");
        }
        try {
            JsonNode data = mapi.call(MapiEndpoint.PRODUCT_LIST, advertiserId,
                    java.util.Map.of("account_id", row.orgId, "account_type", "EBP",
                            "product_platform_id", row.platformId,
                            "filtering", java.util.Map.of("product_id", assetId),
                            "page", 1, "page_size", 10),
                    JsonNode.class);
            JsonNode entry = null;
            for (JsonNode candidate : data.path("list")) {
                if (candidate.path("product_id").asLong() == assetId) {
                    entry = candidate;
                    break;
                }
            }
            if (entry == null) {
                row.deliverable = false;
                row.checkedAt = LocalDateTime.now();
                products.save(row);
                return new SyncOutcome(false, "商品库中查无该商品（未录入升级版库？）");
            }
            boolean deliverable = entry.path("status").asInt() == 1 && entry.path("audit_status").asInt() == 1;
            JsonNode profession = entry.path("profession");
            Long playlet = parseLong(profession.path("playlet_id"));
            Long album = parseLong(profession.path("album_id"));
            if (playlet != null) {
                row.playletId = playlet;
            }
            if (album != null) {
                row.albumId = album;
            }
            row.deliverable = deliverable;
            row.libraryVersion = "UPGRADED";
            row.checkedAt = LocalDateTime.now();
            products.save(row);
            return new SyncOutcome(deliverable, deliverable ? "商品可投放"
                    : "商品不可投: status=" + entry.path("status").asInt()
                    + " audit_status=" + entry.path("audit_status").asInt()
                    + " reason=" + entry.path("audit_reason_text").asText());
        } catch (MapiException e) {
            return new SyncOutcome(false, "商品库读取失败: " + e.getMessage());
        }
    }

    private Long parseLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        try {
            return Long.parseLong(node.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
