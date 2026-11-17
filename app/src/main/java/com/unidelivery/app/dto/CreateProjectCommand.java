package com.unidelivery.app.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 创建全域项目命令。assetId 即商品 ID（related_product.product_id），唯一标的 = awemeId x assetId。
 * confirmStandardStop：业务显式确认「创建全域将停止同标的在投标准项目」（手册硬规则）。
 */
public record CreateProjectCommand(
        long advertiserId,
        long awemeId,
        long assetId,
        long productPlatformId,
        Long playletId,
        String name,
        BigDecimal budget,
        BigDecimal uniRoiGoal,
        String monetizationMode,
        String scheduleType,
        String startTime,
        String endTime,
        String scheduleTime,
        String source,
        List<String> videoIds,
        String imageMode,
        Map<String, Object> brandInfoOverride,
        boolean confirmStandardStop,
        String clientRequestId) {
}
