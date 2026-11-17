package com.unidelivery.app.job;

import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.Advertiser;
import com.unidelivery.app.domain.AwemeAuth;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.repo.AdvertiserRepository;
import com.unidelivery.app.repo.AwemeAuthRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.app.service.AlertService;
import com.unidelivery.app.service.MaterialService;
import com.unidelivery.app.service.OpLogService;
import com.unidelivery.app.service.PreCheckService;
import com.unidelivery.app.service.ProjectService;
import com.unidelivery.app.service.RdsIngestService;
import com.unidelivery.app.service.ReportService;
import com.unidelivery.app.service.SpiBackfillService;
import com.unidelivery.app.service.SubscriptionService;
import com.unidelivery.app.service.TokenService;
import com.unidelivery.app.service.UploadService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务集：token 续期 / 审核轮询 / 上传轮询 / T+1 报表 / 日终对账 / 授权巡检 /
 * 日志回拉对账 / SPI 补拉与积压消费 / RDS 取数。jobs.enabled=false 时全部静默跳过（测试态）。
 * 每个任务独立 catch：单任务失败不拖垮调度线程。
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final UniDeliveryProperties properties;
    private final TokenService tokens;
    private final UniProjectRepository projects;
    private final AdvertiserRepository advertisers;
    private final AwemeAuthRepository awemeAuths;
    private final ProjectService projectService;
    private final MaterialService materials;
    private final PreCheckService preCheck;
    private final UploadService uploads;
    private final ReportService reports;
    private final OpLogService opLogs;
    private final SpiBackfillService backfill;
    private final SubscriptionService subscriptions;
    private final RdsIngestService rds;
    private final AlertService alerts;

    public ScheduledJobs(UniDeliveryProperties properties, TokenService tokens, UniProjectRepository projects,
                         AdvertiserRepository advertisers, AwemeAuthRepository awemeAuths,
                         ProjectService projectService, MaterialService materials, PreCheckService preCheck,
                         UploadService uploads, ReportService reports, OpLogService opLogs,
                         SpiBackfillService backfill, SubscriptionService subscriptions, RdsIngestService rds,
                         AlertService alerts) {
        this.properties = properties;
        this.tokens = tokens;
        this.projects = projects;
        this.advertisers = advertisers;
        this.awemeAuths = awemeAuths;
        this.projectService = projectService;
        this.materials = materials;
        this.preCheck = preCheck;
        this.uploads = uploads;
        this.reports = reports;
        this.opLogs = opLogs;
        this.backfill = backfill;
        this.subscriptions = subscriptions;
        this.rds = rds;
        this.alerts = alerts;
    }

    @Scheduled(fixedDelayString = "${unidelivery.jobs.token-refresh-ms:300000}")
    public void tokenRefresh() {
        if (!enabled()) {
            return;
        }
        run("tokenRefresh", () -> tokens.refreshExpiring());
    }

    @Scheduled(fixedDelayString = "${unidelivery.jobs.review-poll-ms:60000}")
    public void reviewPoll() {
        if (!enabled()) {
            return;
        }
        run("reviewPoll", () -> {
            List<UniProject> reviewing = projects.findByStageIn(
                    List.of(UniProject.Stage.REVIEWING, UniProject.Stage.CREATING));
            for (UniProject project : reviewing) {
                JsonNode entry = projectService.syncFromRemote(project);
                if (entry == null) {
                    continue;
                }
                boolean rejected = false;
                for (JsonNode status : entry.path("status_second")) {
                    if ("PROJECT_STATUS_OFFLINE_AUDIT".equals(status.asText())) {
                        rejected = true;
                    }
                }
                if (rejected) {
                    projectService.rejectReasons(project);
                    alerts.alert("ERROR", "项目审核不通过",
                            "project_id=" + project.projectId + " 见 reject_reason_json");
                }
                if (project.stage == UniProject.Stage.ACTIVE) {
                    materials.reconcile(project);
                }
            }
            return reviewing.size();
        });
    }

    @Scheduled(fixedDelayString = "${unidelivery.jobs.upload-poll-ms:30000}")
    public void uploadPoll() {
        if (!enabled()) {
            return;
        }
        run("uploadPoll", uploads::pollTasks);
    }

    @Scheduled(cron = "${unidelivery.jobs.report-pull-cron:0 30 1 * * *}")
    public void reportPull() {
        if (!enabled()) {
            return;
        }
        run("reportPull", () -> {
            LocalDateTime dayStart = LocalDate.now().minusDays(1).atStartOfDay();
            int rows = 0;
            for (Advertiser advertiser : advertisers.findByWhitelistedTrue()) {
                for (UniDeliveryProperties.TopicSpec spec : properties.reports()) {
                    rows += reports.pull(advertiser.advertiserId, spec.topic(), spec.dimensions(),
                            spec.metrics(), dayStart, dayStart.plusDays(1), "OFFLINE", null);
                }
            }
            return rows;
        });
    }

    @Scheduled(cron = "${unidelivery.jobs.reconcile-cron:0 0 3 * * *}")
    public void reconcile() {
        if (!enabled()) {
            return;
        }
        run("reconcile", () -> {
            LocalDateTime dayStart = LocalDate.now().minusDays(1).atStartOfDay();
            int breaches = 0;
            for (Advertiser advertiser : advertisers.findByWhitelistedTrue()) {
                for (UniDeliveryProperties.TopicSpec spec : properties.reports()) {
                    breaches += reports.reconcile(advertiser.advertiserId, spec.topic(), dayStart).size();
                }
            }
            return breaches;
        });
    }

    @Scheduled(cron = "${unidelivery.jobs.auth-patrol-cron:0 0 8 * * *}")
    public void authPatrol() {
        if (!enabled()) {
            return;
        }
        run("authPatrol", () -> {
            int invalid = 0;
            for (AwemeAuth auth : awemeAuths.findAll()) {
                var item = preCheck.patrolAuth(auth.advertiserId, auth.awemeId);
                if (!item.passed()) {
                    invalid++;
                    alerts.alert("ERROR", "抖音号全域授权失效",
                            "aweme=" + auth.awemeId + " " + item.reason());
                }
            }
            return invalid;
        });
    }

    @Scheduled(fixedDelayString = "${unidelivery.jobs.log-sync-ms:300000}")
    public void logSync() {
        if (!enabled()) {
            return;
        }
        run("logSync", () -> {
            LocalDateTime until = LocalDateTime.now();
            LocalDateTime since = until.minusHours(1);
            int synced = 0;
            for (Advertiser advertiser : advertisers.findByWhitelistedTrue()) {
                synced += opLogs.sync(advertiser.advertiserId, since, until);
                opLogs.reconcile(advertiser.advertiserId, since, until);
            }
            return synced;
        });
    }

    @Scheduled(fixedDelayString = "${unidelivery.jobs.review-poll-ms:60000}")
    public void spiMaintenance() {
        if (!enabled()) {
            return;
        }
        run("spiBackfill", backfill::backfill);
        run("spiConsumePending", subscriptions::consumePending);
        run("rdsIngest", rds::ingest);
    }

    private boolean enabled() {
        return properties.jobs().enabled();
    }

    private void run(String name, java.util.concurrent.Callable<?> task) {
        try {
            Object result = task.call();
            log.debug("job {} 完成: {}", name, result);
        } catch (Exception e) {
            log.error("job {} 失败: {}", name, e.getMessage());
        }
    }
}
