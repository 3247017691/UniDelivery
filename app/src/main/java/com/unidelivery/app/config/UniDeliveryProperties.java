package com.unidelivery.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "unidelivery")
public record UniDeliveryProperties(
        @DefaultValue("") String appId,
        @DefaultValue("") String appSecret,
        @DefaultValue("200") int chunkSize,
        @DefaultValue("0.01") double reconcileThreshold,
        @DefaultValue("") String spiSecret,
        @DefaultValue("rejectAllSpiVerifier") String spiVerifier,
        @DefaultValue("") String alertWebhookUrl,
        @DefaultValue java.util.List<TopicSpec> reports,
        @DefaultValue Rds rds,
        @DefaultValue Spi spi,
        @DefaultValue Jobs jobs) {

    /** 报表口径：维度/指标以 config/get 实机返回或平台 UI 生成为准，显式配置后才拉取。 */
    public record TopicSpec(
            String topic,
            java.util.List<String> dimensions,
            java.util.List<String> metrics,
            @DefaultValue("HOUR") String granularity) {
    }

    /** RDS：连山云 MySQL（北京机房），库 lianshan_rds_{taskId}，按周分表；未配置则 job 跳过。 */
    public record Rds(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("") String url,
            @DefaultValue("") String username,
            @DefaultValue("") String password,
            @DefaultValue("0") long taskId,
            @DefaultValue("UNI_PROJECT_DATA") String topic) {
    }

    public record Spi(@DefaultValue java.util.List<Long> subscribeTaskIds) {
    }

    public record Jobs(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("300000") long tokenRefreshMs,
            @DefaultValue("60000") long reviewPollMs,
            @DefaultValue("30000") long uploadPollMs,
            @DefaultValue("0 30 1 * * *") String reportPullCron,
            @DefaultValue("0 0 3 * * *") String reconcileCron,
            @DefaultValue("0 0 8 * * *") String authPatrolCron,
            @DefaultValue("300000") long logSyncMs) {
    }
}
