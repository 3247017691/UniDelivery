package com.unidelivery.app.e2e;

import com.unidelivery.app.domain.ReportSnapshot;
import com.unidelivery.app.repo.ReportSnapshotRepository;
import com.unidelivery.app.service.RdsIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RDS 取数契约：连山 MySQL 周分表 + 指标平铺列 → REALTIME 快照。
 * 用 H2 扮演连山库（同库建 lianshan_realtime_{周一} 表），验证通用列映射与稳定小时窗口。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e_rds;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "unidelivery.rds.enabled=true",
        "unidelivery.rds.url=jdbc:h2:mem:e2e_rds;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "unidelivery.rds.username=sa",
        "unidelivery.rds.password=",
        "unidelivery.rds.task-id=1731339863784456",
        "unidelivery.rds.topic=UNI_PROJECT_DATA"})
class RdsIngestTest {

    private static final DateTimeFormatter TABLE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RdsIngestService rds;
    @Autowired
    private ReportSnapshotRepository snapshots;

    @Test
    void weeklyTableFlatMetricsIngestedAsRealtimeSnapshots() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        String table = "lianshan_realtime_" + weekStart.format(TABLE_DATE);
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "id BIGINT PRIMARY KEY, advertiser_id BIGINT, project_id BIGINT, material_id BIGINT,"
                + "stat_time TIMESTAMP, create_time TIMESTAMP, update_time TIMESTAMP, uid BIGINT,"
                + "`show` BIGINT, stat_cost DOUBLE)");
        LocalDateTime stableBefore = LocalDateTime.now().minusMinutes(15).truncatedTo(ChronoUnit.HOURS)
                .plusHours(1);
        LocalDateTime stat = LocalDateTime.now().minusMinutes(20).truncatedTo(ChronoUnit.HOURS);
        if (stat.isBefore(weekStart.atStartOfDay())) {
            stat = weekStart.atStartOfDay();
        }
        boolean expectIngest = stat.isBefore(stableBefore);
        jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO " + table + " VALUES (1,1001,555,0,'" + stat.format(TS) + "','"
                + LocalDateTime.now().format(TS) + "','" + LocalDateTime.now().format(TS) + "',9,100,12.5)");

        int ingested = rds.ingest();
        List<ReportSnapshot> rows = snapshots.findByAdvertiserIdAndTopicAndChannelAndWindowStartBetween(
                1001L, "UNI_PROJECT_DATA", "REALTIME", weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());
        if (expectIngest) {
            assertEquals(1, ingested);
            assertEquals(1, rows.size());
            assertEquals(stat, rows.get(0).windowStart);
            assertTrue(rows.get(0).metricsJson.contains("stat_cost"));
            assertTrue(rows.get(0).metricsJson.contains("show"));
            assertTrue(!rows.get(0).metricsJson.contains("advertiser_id"), "维度列不混入指标");
        } else {
            assertEquals(0, ingested);
        }
    }
}
