package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.domain.SubscriptionEvent;
import com.unidelivery.app.repo.SubscriptionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SPI 事件接收与消费：message_id 唯一键去重（at-least-once → 幂等）；
 * 推送为「数据就绪」消息（content.project_ids / material_ids），消费 = 触发实时报表拉取；
 * 报表口径未配置时事件保持未消费，由 consumePending 任务重试，不丢事件。
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionEventRepository events;
    private final ReportService reports;
    private final ObjectMapper mapper = new ObjectMapper();

    public SubscriptionService(SubscriptionEventRepository events, ReportService reports) {
        this.events = events;
        this.reports = reports;
    }

    public record ReceiveResult(boolean duplicate, boolean consumed, String eventId) {
    }

    @Transactional
    public ReceiveResult receive(JsonNode envelope) {
        String messageId = envelope.path("message_id").asText("");
        if (messageId.isBlank()) {
            throw new IllegalArgumentException("SPI 封套缺少 message_id");
        }
        if (events.findByEventId(messageId).isPresent()) {
            return new ReceiveResult(true, true, messageId);
        }
        SubscriptionEvent event = new SubscriptionEvent();
        event.eventId = messageId;
        event.eventType = envelope.path("service_label").asText("");
        event.payloadJson = envelope.toString();
        event.consumed = false;
        event.receivedAt = LocalDateTime.now();
        events.save(event);
        boolean consumed = consume(event);
        return new ReceiveResult(false, consumed, messageId);
    }

    /** 幂等消费；返回 false 表示待重试（口径未配置等）。 */
    @Transactional
    public boolean consume(SubscriptionEvent event) {
        if (event.consumed) {
            return true;
        }
        try {
            JsonNode envelope = mapper.readTree(event.payloadJson);
            String topic = topicOf(event.eventType);
            if (topic == null) {
                log.info("非本系统订阅服务类型，忽略: {}", event.eventType);
                markConsumed(event);
                return true;
            }
            JsonNode data = envelope.path("data");
            if (data.isTextual()) {
                data = mapper.readTree(data.asText());
            }
            long advertiserId = envelope.path("advertiser_ids").path(0).asLong(
                    data.path("user_id").asLong(0));
            JsonNode content = data.path("content");
            List<Long> projectIds = ids(content.path("project_ids"));
            List<Long> materialIds = ids(content.path("material_ids"));
            int pulled = reports.pullRealtimeForIds(advertiserId, topic, projectIds, materialIds);
            if (pulled < 0) {
                return false;
            }
            markConsumed(event);
            return true;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("SPI payload 解析失败 event={}: {}", event.eventId, e.getMessage());
            markConsumed(event);
            return true;
        }
    }

    @Transactional
    public int consumePending() {
        int consumed = 0;
        for (SubscriptionEvent event : events.findByConsumedFalse()) {
            if (consume(event)) {
                consumed++;
            }
        }
        return consumed;
    }

    private void markConsumed(SubscriptionEvent event) {
        event.consumed = true;
        event.consumedAt = LocalDateTime.now();
        events.save(event);
    }

    private String topicOf(String serviceLabel) {
        return switch (serviceLabel) {
            case "report.uniproject.activeprogram" -> "UNI_PROJECT_DATA";
            case "report.unimaterial.activeprogram" -> "UNI_PROJECT_MATERIAL_DATA";
            default -> null;
        };
    }

    private List<Long> ids(JsonNode node) {
        List<Long> ids = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(n -> ids.add(n.asLong()));
        }
        return ids;
    }
}
