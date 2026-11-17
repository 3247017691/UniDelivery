package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.ReportSnapshot;
import com.unidelivery.app.repo.ReportSnapshotRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 报表双通道：离线 T+1 拉取（report/custom/get，data_topic 单数——契约核实）+ 实时通道 upsert；
 * 日终对账（同窗口同指标相对差 ≤ 阈值）。维度/指标口径由配置或调用方显式给出（以 config/get 实机为准）。
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGES = 100;

    private final MapiClient mapi;
    private final ReportSnapshotRepository snapshots;
    private final UniDeliveryProperties properties;
    private final AlertService alerts;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportService(MapiClient mapi, ReportSnapshotRepository snapshots,
                         UniDeliveryProperties properties, AlertService alerts) {
        this.mapi = mapi;
        this.snapshots = snapshots;
        this.properties = properties;
        this.alerts = alerts;
    }

    /** 可用指标/维度（config/get，data_topics 复数）；用于口径核定与枚举校验。 */
    public JsonNode config(long advertiserId, List<String> topics) {
        return mapi.call(MapiEndpoint.REPORT_CONFIG_GET, advertiserId,
                Map.of("advertiser_id", advertiserId, "data_topics", topics), JsonNode.class);
    }

    public Optional<UniDeliveryProperties.TopicSpec> spec(String topic) {
        return properties.reports().stream().filter(s -> s.topic().equals(topic)).findFirst();
    }

    /** 拉取并落 OFFLINE 快照；dimensions/metrics 显式传入（平台 UI 生成或 config/get 核定）。 */
    @Transactional
    public int pull(long advertiserId, String topic, List<String> dimensions, List<String> metrics,
                    LocalDateTime start, LocalDateTime end, String channel, List<Map<String, Object>> filters) {
        int rows = 0;
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("advertiser_id", advertiserId);
            params.put("data_topic", topic);
            params.put("dimensions", dimensions);
            params.put("metrics", metrics);
            params.put("start_time", start.format(TS));
            params.put("end_time", end.format(TS));
            params.put("order_by", List.of(Map.of("field", metrics.get(0), "type", "DESC")));
            if (filters != null && !filters.isEmpty()) {
                params.put("filters", filters);
            }
            params.put("page", page);
            params.put("page_size", 100);
            JsonNode data = mapi.call(MapiEndpoint.REPORT_CUSTOM_GET, advertiserId, params, JsonNode.class);
            for (JsonNode row : data.path("rows")) {
                upsert(advertiserId, topic, channel, row, start);
                rows++;
            }
            if (page >= data.path("page_info").path("total_page").asInt(1)) {
                break;
            }
        }
        return rows;
    }

    /** SPI「数据就绪」消息触发的实时拉取：按变更 ID 过滤当前小时窗口。 */
    @Transactional
    public int pullRealtimeForIds(long advertiserId, String topic, List<Long> projectIds, List<Long> materialIds) {
        Optional<UniDeliveryProperties.TopicSpec> spec = spec(topic);
        if (spec.isEmpty()) {
            log.warn("报表口径未配置，topic={} 实时拉取跳过（待配置后消费积压事件）", topic);
            return -1;
        }
        List<Map<String, Object>> filters = new ArrayList<>();
        if (projectIds != null && !projectIds.isEmpty()) {
            filters.add(Map.of("field", "project_id", "type", 1, "operator", 10,
                    "values", projectIds.stream().map(String::valueOf).toList()));
        }
        if (materialIds != null && !materialIds.isEmpty()) {
            filters.add(Map.of("field", "material_id", "type", 1, "operator", 10,
                    "values", materialIds.stream().map(String::valueOf).toList()));
        }
        LocalDateTime hourStart = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return pull(advertiserId, topic, spec.get().dimensions(), spec.get().metrics(),
                hourStart, LocalDateTime.now(), "REALTIME", filters);
    }

    /** 日终对账：同窗口 REALTIME vs OFFLINE 共有数值指标相对差；超阈值告警。返回超阈值条目。 */
    public List<String> reconcile(long advertiserId, String topic, LocalDateTime dayStart) {
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<ReportSnapshot> realtime = snapshots.findByAdvertiserIdAndTopicAndChannelAndWindowStartBetween(
                advertiserId, topic, "REALTIME", dayStart, dayEnd);
        List<ReportSnapshot> offline = snapshots.findByAdvertiserIdAndTopicAndChannelAndWindowStartBetween(
                advertiserId, topic, "OFFLINE", dayStart, dayEnd);
        Map<String, JsonNode> offlineByKey = new LinkedHashMap<>();
        for (ReportSnapshot snapshot : offline) {
            offlineByKey.put(key(snapshot), parse(snapshot.metricsJson));
        }
        List<String> breaches = new ArrayList<>();
        for (ReportSnapshot snapshot : realtime) {
            JsonNode off = offlineByKey.get(key(snapshot));
            if (off == null) {
                breaches.add("缺离线对照: " + key(snapshot));
                continue;
            }
            JsonNode real = parse(snapshot.metricsJson);
            real.fieldNames().forEachRemaining(field -> {
                if (!off.has(field) || !real.path(field).isNumber() || !off.path(field).isNumber()) {
                    return;
                }
                double r = real.path(field).asDouble();
                double o = off.path(field).asDouble();
                double base = Math.max(Math.abs(o), 1e-9);
                if (Math.abs(r - o) / base > properties.reconcileThreshold()) {
                    breaches.add(key(snapshot) + " " + field + " realtime=" + r + " offline=" + o);
                }
            });
        }
        if (!breaches.isEmpty()) {
            alerts.alert("WARN", "报表日终对账超阈值", "topic=" + topic + " breaches=" + breaches);
        }
        return breaches;
    }

    private void upsert(long advertiserId, String topic, String channel, JsonNode row, LocalDateTime fallback) {
        JsonNode dims = row.path("dimensions");
        JsonNode metrics = row.path("metrics");
        long projectId = dims.path("project_id").asLong(0);
        long materialId = dims.path("material_id").asLong(0);
        LocalDateTime window = parseWindow(dims, fallback);
        String granularity = properties.reports().stream()
                .filter(s -> s.topic().equals(topic)).findFirst()
                .map(UniDeliveryProperties.TopicSpec::granularity).orElse("HOUR");
        ReportSnapshot snapshot = snapshots
                .findByAdvertiserIdAndTopicAndChannelAndGranularityAndWindowStartAndProjectIdAndMaterialId(
                        advertiserId, topic, channel, granularity, window, projectId, materialId)
                .orElseGet(() -> {
                    ReportSnapshot created = new ReportSnapshot();
                    created.advertiserId = advertiserId;
                    created.topic = topic;
                    created.channel = channel;
                    created.granularity = granularity;
                    created.windowStart = window;
                    created.projectId = projectId;
                    created.materialId = materialId;
                    created.createdAt = LocalDateTime.now();
                    return created;
                });
        snapshot.metricsJson = metrics.toString();
        snapshots.save(snapshot);
    }

    private LocalDateTime parseWindow(JsonNode dims, LocalDateTime fallback) {
        for (String field : List.of("stat_time", "stat_datetime", "time")) {
            JsonNode node = dims.path(field);
            if (node.isTextual()) {
                try {
                    return LocalDateTime.parse(node.asText(), TS);
                } catch (java.time.format.DateTimeParseException ignored) {
                    // 尝试下一种格式/字段
                }
            }
        }
        return fallback.truncatedTo(ChronoUnit.HOURS);
    }

    private String key(ReportSnapshot snapshot) {
        return snapshot.windowStart + "|p=" + snapshot.projectId + "|m=" + snapshot.materialId;
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("metrics_json 解析失败", e);
        }
    }
}
