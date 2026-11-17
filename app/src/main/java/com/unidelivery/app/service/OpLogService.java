package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.OpJournal;
import com.unidelivery.app.domain.PlatformLog;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.repo.OpJournalRepository;
import com.unidelivery.app.repo.PlatformLogRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 平台操作日志回拉 + 大日志展开（second_log_id → detail/get，契约核实字段名）+
 * 「本地意图 ↔ 平台事实」双向对账：平台有操作而本地无 journal → 告警。
 */
@Service
public class OpLogService {

    private static final Logger log = LoggerFactory.getLogger(OpLogService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGES = 20;

    private final MapiClient mapi;
    private final PlatformLogRepository logs;
    private final OpJournalRepository journals;
    private final UniProjectRepository projects;
    private final AlertService alerts;

    public OpLogService(MapiClient mapi, PlatformLogRepository logs, OpJournalRepository journals,
                        UniProjectRepository projects, AlertService alerts) {
        this.mapi = mapi;
        this.logs = logs;
        this.journals = journals;
        this.projects = projects;
        this.alerts = alerts;
    }

    @Transactional
    public int sync(long advertiserId, LocalDateTime since, LocalDateTime until) {
        int synced = 0;
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode data = mapi.call(MapiEndpoint.LOG_SEARCH, advertiserId,
                    Map.of("advertiser_id", advertiserId,
                            "start_time", since.format(TS), "end_time", until.format(TS),
                            "page", page, "page_size", 100),
                    JsonNode.class);
            for (JsonNode entry : data.path("logs")) {
                LocalDateTime opTime = parseTime(entry.path("create_time").asText());
                if (opTime == null) {
                    continue;
                }
                String title = entry.path("content_title").asText("");
                long objectId = entry.path("object_id").asLong();
                PlatformLog row = logs.findByAdvertiserIdAndObjectIdAndOpTimeAndContentTitle(
                        advertiserId, objectId, opTime, title).orElseGet(() -> {
                    PlatformLog created = new PlatformLog();
                    created.advertiserId = advertiserId;
                    created.objectId = objectId;
                    created.opTime = opTime;
                    created.contentTitle = title;
                    return created;
                });
                row.objectType = entry.path("object_type").asText();
                JsonNode secondLogId = entry.path("second_log_id");
                if (secondLogId.isNumber()) {
                    row.secondLogId = secondLogId.asLong();
                    JsonNode detail = mapi.call(MapiEndpoint.LOG_DETAIL, advertiserId,
                            Map.of("advertiser_id", advertiserId, "second_log_id", row.secondLogId),
                            JsonNode.class);
                    row.detailJson = detail.path("log").toString();
                }
                row.syncedAt = LocalDateTime.now();
                logs.save(row);
                synced++;
            }
            if (page >= data.path("page_info").path("total_page").asInt(1)) {
                break;
            }
        }
        return synced;
    }

    /** 平台操作 vs 本地 journal：对象为本系统项目且时间窗内无对应写 journal → 未追溯操作告警。 */
    public List<String> reconcile(long advertiserId, LocalDateTime since, LocalDateTime until) {
        List<Long> projectIds = projects.findByAdvertiserId(advertiserId).stream()
                .map(p -> p.projectId).filter(java.util.Objects::nonNull).toList();
        List<OpJournal> writeJournals = journals.findByStatus("OK").stream()
                .filter(j -> j.advertiserId.equals(advertiserId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        writeJournals.addAll(journals.findByStatus("PARTIAL"));
        writeJournals.addAll(journals.findByStatus("ADOPTED"));
        List<String> untracked = new ArrayList<>();
        for (PlatformLog row : logs.findByAdvertiserIdAndOpTimeBetween(advertiserId, since, until)) {
            if (!projectIds.contains(row.objectId)) {
                continue;
            }
            Long localId = projects.findByProjectId(row.objectId).map(p -> p.id).orElse(null);
            boolean matched = writeJournals.stream().anyMatch(j ->
                    localId != null && localId.equals(j.projectLocalId)
                            && j.createdAt != null
                            && j.createdAt.isAfter(row.opTime.minusMinutes(10))
                            && j.createdAt.isBefore(row.opTime.plusMinutes(10)));
            if (!matched) {
                untracked.add("object_id=" + row.objectId + " op_time=" + row.opTime
                        + " title=" + row.contentTitle);
            }
        }
        if (!untracked.isEmpty()) {
            alerts.alert("WARN", "平台存在未追溯操作", "adv=" + advertiserId + " " + untracked);
        }
        return untracked;
    }

    private LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, TS);
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("平台日志时间解析失败: {}", text);
            return null;
        }
    }
}
