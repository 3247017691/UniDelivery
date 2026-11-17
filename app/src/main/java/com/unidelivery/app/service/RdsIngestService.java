package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.ReportSnapshot;
import com.unidelivery.app.repo.ReportSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * RDS 实时报表取数（契约核实：连山云 MySQL 直推，非 HTTP 回调）：
 * 库 lianshan_rds_{taskId}（url 配置含库名），表 lianshan_realtime_{当周周一 yyyyMMdd} 按周分表；
 * 指标平铺列 → metrics_json；整点后 15 分钟稳定、次日 9 点修正（取数窗口留 15 分钟滞后）。
 * 未配置 enabled=false 时跳过并如实日志，不假装已接入。
 */
@Service
public class RdsIngestService {

    private static final Logger log = LoggerFactory.getLogger(RdsIngestService.class);
    private static final DateTimeFormatter TABLE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> NON_METRIC_COLUMNS =
            Set.of("id", "uid", "stat_time", "create_time", "update_time");
    private static final Set<String> DIMENSION_COLUMNS =
            Set.of("advertiser_id", "project_id", "material_id", "creative_id", "inventory");

    private final UniDeliveryProperties properties;
    private final ReportSnapshotRepository snapshots;
    private final ObjectMapper mapper = new ObjectMapper();

    public RdsIngestService(UniDeliveryProperties properties, ReportSnapshotRepository snapshots) {
        this.properties = properties;
        this.snapshots = snapshots;
    }

    public int ingest() {
        UniDeliveryProperties.Rds rds = properties.rds();
        if (!rds.enabled()) {
            log.debug("RDS 未配置，跳过取数");
            return 0;
        }
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        String table = "lianshan_realtime_" + weekStart.format(TABLE_DATE);
        LocalDateTime stableBefore = LocalDateTime.now().minusMinutes(15).truncatedTo(ChronoUnit.HOURS)
                .plusHours(1);
        int ingested = 0;
        String sql = "SELECT * FROM " + table + " WHERE stat_time >= '"
                + weekStart.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + "' AND stat_time < '"
                + stableBefore.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'";
        try (Connection connection = DriverManager.getConnection(rds.url(), rds.username(), rds.password());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> metrics = new LinkedHashMap<>();
                long advertiserId = 0;
                long projectId = 0;
                long materialId = 0;
                LocalDateTime statTime = null;
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String column = meta.getColumnLabel(i).toLowerCase();
                    Object value = rs.getObject(i);
                    if ("stat_time".equals(column) && value instanceof Timestamp ts) {
                        statTime = ts.toLocalDateTime();
                    }
                    if ("advertiser_id".equals(column) && value instanceof Number n) {
                        advertiserId = n.longValue();
                    }
                    if ("project_id".equals(column) && value instanceof Number n) {
                        projectId = n.longValue();
                    }
                    if ("material_id".equals(column) && value instanceof Number n) {
                        materialId = n.longValue();
                    }
                    if (!NON_METRIC_COLUMNS.contains(column) && !DIMENSION_COLUMNS.contains(column)
                            && value != null) {
                        metrics.put(column, value);
                    }
                }
                if (statTime == null || advertiserId == 0) {
                    continue;
                }
                LocalDateTime window = statTime.truncatedTo(ChronoUnit.HOURS);
                final long advKey = advertiserId;
                final long projectKey = projectId;
                final long materialKey = materialId;
                ReportSnapshot snapshot = snapshots
                        .findByAdvertiserIdAndTopicAndChannelAndGranularityAndWindowStartAndProjectIdAndMaterialId(
                                advKey, rds.topic(), "REALTIME", "HOUR", window, projectKey, materialKey)
                        .orElseGet(() -> {
                            ReportSnapshot created = new ReportSnapshot();
                            created.advertiserId = advKey;
                            created.topic = rds.topic();
                            created.channel = "REALTIME";
                            created.granularity = "HOUR";
                            created.windowStart = window;
                            created.projectId = projectKey;
                            created.materialId = materialKey;
                            created.createdAt = LocalDateTime.now();
                            return created;
                        });
                snapshot.metricsJson = mapper.writeValueAsString(metrics);
                snapshots.save(snapshot);
                ingested++;
            }
        } catch (Exception e) {
            log.error("RDS 取数失败（table={}）: {}", table, e.getMessage());
            return -1;
        }
        return ingested;
    }
}
