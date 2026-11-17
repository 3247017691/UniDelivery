package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.OpJournal;
import com.unidelivery.app.dto.BatchOutcome;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiException;
import com.unidelivery.mapi.MapiResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 投中调控：启停/ROI（兼容接口，单批 ≤10）、变现模式（IAA_AND_IAP 必带 IAA 链接）、
 * 删除/投放日期/时段（字段契约未核实 → 受审计透传通道，payload 由操作者按官方文档提供）。
 */
@Service
public class DeliveryService {

    private static final int BATCH = 10;

    private final MapiClient mapi;
    private final UniProjectRepository projects;
    private final JournalService journal;
    private final AlertService alerts;

    public DeliveryService(MapiClient mapi, UniProjectRepository projects, JournalService journal,
                           AlertService alerts) {
        this.mapi = mapi;
        this.projects = projects;
        this.journal = journal;
        this.alerts = alerts;
    }

    @Transactional
    public BatchOutcome updateStatus(long advertiserId, List<Long> projectIds, String optStatus,
                                     String clientRequestId) {
        if (!"ENABLE".equals(optStatus) && !"DISABLE".equals(optStatus)) {
            throw new IllegalArgumentException("opt_status 仅允许 ENABLE/DISABLE: " + optStatus);
        }
        return batchCall(MapiEndpoint.PROJECT_STATUS_UPDATE, advertiserId, projectIds, clientRequestId,
                chunk -> {
                    List<Map<String, Object>> data = new ArrayList<>();
                    for (Long id : chunk) {
                        data.add(Map.of("project_id", id, "opt_status", optStatus));
                    }
                    return Map.of("advertiser_id", advertiserId, "data", data);
                });
    }

    @Transactional
    public BatchOutcome updateRoiGoal(long advertiserId, Map<Long, BigDecimal> roiByProject,
                                      String clientRequestId) {
        for (Map.Entry<Long, BigDecimal> entry : roiByProject.entrySet()) {
            BigDecimal roi = entry.getValue();
            if (roi.compareTo(new BigDecimal("0.01")) < 0 || roi.compareTo(new BigDecimal("5")) > 0
                    || roi.scale() > 4) {
                throw new IllegalArgumentException("全域 ROI 系数范围 [0.01,5] 且最多 4 位小数: " + roi);
            }
        }
        List<Long> ids = new ArrayList<>(roiByProject.keySet());
        return batchCall(MapiEndpoint.PROJECT_ROIGOAL_UPDATE, advertiserId, ids, clientRequestId,
                chunk -> {
                    List<Map<String, Object>> data = new ArrayList<>();
                    for (Long id : chunk) {
                        data.add(Map.of("project_id", id, "roi_goal", roiByProject.get(id)));
                    }
                    return Map.of("advertiser_id", advertiserId, "data", data);
                });
    }

    /** 变现模式批量更新：正文允许值 IAP/IAA_AND_IAP；混合变现必带 IAA 链接（传 IAP 链接会失败）。 */
    @Transactional
    public BatchOutcome updateMonetization(long advertiserId, List<MonetizationChange> changes,
                                           String clientRequestId) {
        List<Map<String, Object>> projectList = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (MonetizationChange change : changes) {
            if (!"IAP".equals(change.mode()) && !"IAA_AND_IAP".equals(change.mode())) {
                throw new IllegalArgumentException("monetization_mode 仅允许 IAP/IAA_AND_IAP: " + change.mode());
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("project_id", change.projectId());
            item.put("monetization_mode", change.mode());
            if ("IAA_AND_IAP".equals(change.mode())) {
                if (change.iaaUrl() == null || change.iaaUrl().isBlank()) {
                    throw new IllegalArgumentException("混合变现必须传入 IAA 专辑链接: project=" + change.projectId());
                }
                item.put("playlet_series_url", change.iaaUrl());
            }
            projectList.add(item);
            ids.add(change.projectId());
        }
        String requestId = clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId;
        OpJournal entry = journal.begin(requestId, MapiEndpoint.UNI_MONETIZATION_UPDATE, advertiserId, null,
                "monetization projects=" + ids);
        MapiResult result;
        try {
            result = mapi.call(MapiEndpoint.UNI_MONETIZATION_UPDATE, advertiserId,
                    Map.of("advertiser_id", advertiserId, "project_list", projectList));
        } catch (MapiException e) {
            journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
            throw e;
        }
        BatchOutcome outcome = parseOutcome(result.data(), "project_id");
        for (Long id : outcome.successIds()) {
            projects.findByProjectId(id).ifPresent(p -> {
                changes.stream().filter(c -> c.projectId().equals(id)).findFirst().ifPresent(c -> {
                    p.monetizationMode = c.mode();
                    p.updatedAt = java.time.LocalDateTime.now();
                    projects.save(p);
                });
            });
        }
        finishBatch(entry, result, outcome);
        return outcome;
    }

    /** 受审计透传：删除/投放日期/投放时段等字段契约未核实接口，payload 由操作者按官方文档组装。 */
    @Transactional
    public JsonNode auditedPassthrough(MapiEndpoint endpoint, long advertiserId, Map<String, Object> payload,
                                       String clientRequestId, Long projectLocalId) {
        if (!endpoint.write()) {
            throw new IllegalArgumentException("透传通道仅接受写接口: " + endpoint);
        }
        String requestId = clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId;
        OpJournal entry = journal.begin(requestId, endpoint, advertiserId, projectLocalId, payload.toString());
        MapiResult result;
        try {
            result = mapi.call(endpoint, advertiserId, payload);
        } catch (MapiException e) {
            journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
            throw e;
        }
        journal.finish(entry, "OK", result.requestId(), result.data().toString());
        return result.data();
    }

    public record MonetizationChange(Long projectId, String mode, String iaaUrl) {
    }

    private BatchOutcome batchCall(MapiEndpoint endpoint, long advertiserId, List<Long> ids,
                                   String clientRequestId,
                                   java.util.function.Function<List<Long>, Map<String, Object>> payloadBuilder) {
        String requestId = clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId;
        List<Long> success = new ArrayList<>();
        List<BatchOutcome.ErrorEntry> errors = new ArrayList<>();
        List<List<Long>> chunks = ProjectService.chunk(ids, BATCH);
        for (int i = 0; i < chunks.size(); i++) {
            OpJournal entry = journal.begin(requestId + "-" + (i + 1), endpoint, advertiserId, null,
                    endpoint.name() + " ids=" + chunks.get(i));
            MapiResult result;
            try {
                result = mapi.call(endpoint, advertiserId, payloadBuilder.apply(chunks.get(i)));
            } catch (MapiException e) {
                journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
                throw e;
            }
            BatchOutcome outcome = parseOutcome(result.data(), "project_id");
            success.addAll(outcome.successIds());
            errors.addAll(outcome.errors());
            finishBatch(entry, result, outcome);
        }
        if (!errors.isEmpty()) {
            alerts.alert("WARN", "批量调控部分失败", endpoint.name() + " errors=" + errors);
        }
        return new BatchOutcome(success, errors);
    }

    private BatchOutcome parseOutcome(JsonNode data, String idField) {
        List<Long> success = new ArrayList<>();
        List<BatchOutcome.ErrorEntry> errors = new ArrayList<>();
        data.path("project_ids").forEach(n -> success.add(n.asLong()));
        data.path("errors").forEach(n -> errors.add(new BatchOutcome.ErrorEntry(
                n.path(idField).asLong(), n.path("error_message").asText())));
        return new BatchOutcome(success, errors);
    }

    private void finishBatch(OpJournal entry, MapiResult result, BatchOutcome outcome) {
        journal.finish(entry, outcome.fullSuccess() ? "OK" : "PARTIAL", result.requestId(),
                result.data().toString());
    }
}
