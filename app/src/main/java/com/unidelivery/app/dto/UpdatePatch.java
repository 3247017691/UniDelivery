package com.unidelivery.app.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 增量更新补丁：仅非 null 字段进入请求体（field-mask），禁止零值默认序列化误改。
 * 空值语义官方未说明（契约未核实清单），故不支持「传空清空」操作。
 */
public record UpdatePatch(
        String name,
        BigDecimal budget,
        BigDecimal uniRoiGoal,
        String monetizationMode,
        String scheduleType,
        String endTime,
        String scheduleTime,
        String aigcDynamicCreativeSwitch,
        Map<String, Object> brandInfo,
        Map<String, Object> audience) {
}
