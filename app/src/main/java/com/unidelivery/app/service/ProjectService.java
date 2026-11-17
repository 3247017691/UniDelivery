package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.OpJournal;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.dto.CreateProjectCommand;
import com.unidelivery.app.dto.PreCheckResult;
import com.unidelivery.app.dto.UpdatePatch;
import com.unidelivery.app.repo.AdvertiserRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiException;
import com.unidelivery.mapi.MapiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 全域项目生命周期：预检闸门 → 唯一标的互斥 → 标准停投二次确认 → 200 分批创建 →
 * 超时先查后认领（幂等）→ 增量编辑（field-mask）→ 状态/审核同步。
 * 部分成功（error_list 非空）不算全成功：journal=PARTIAL + 告警。
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_IMAGE_MODE = "CREATIVE_IMAGE_MODE_VIDEO_VERTICAL";

    private final MapiClient mapi;
    private final UniProjectRepository projects;
    private final AdvertiserRepository advertisers;
    private final PreCheckService preCheck;
    private final MaterialService materials;
    private final JournalService journal;
    private final AlertService alerts;
    private final UniDeliveryProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProjectService(MapiClient mapi, UniProjectRepository projects, AdvertiserRepository advertisers,
                          PreCheckService preCheck, MaterialService materials, JournalService journal,
                          AlertService alerts, UniDeliveryProperties properties) {
        this.mapi = mapi;
        this.projects = projects;
        this.advertisers = advertisers;
        this.preCheck = preCheck;
        this.materials = materials;
        this.journal = journal;
        this.alerts = alerts;
        this.properties = properties;
    }

    @Transactional
    public UniProject create(CreateProjectCommand cmd) {
        String requestId = cmd.clientRequestId() == null || cmd.clientRequestId().isBlank()
                ? UUID.randomUUID().toString() : cmd.clientRequestId();
        OpJournal entry = journal.begin(requestId, MapiEndpoint.UNI_PROJECT_CREATE, cmd.advertiserId(), null,
                "create aweme=" + cmd.awemeId() + " asset=" + cmd.assetId() + " videos=" + cmd.videoIds().size());
        if (("OK".equals(entry.status) || "ADOPTED".equals(entry.status) || "PARTIAL".equals(entry.status))
                && entry.projectLocalId != null) {
            return projects.findById(entry.projectLocalId).orElseThrow();
        }
        Long playletId = cmd.playletId();
        PreCheckResult check = preCheck.run(cmd.advertiserId(), cmd.awemeId(), cmd.assetId(), playletId);
        if (!check.allPassed()) {
            journal.finish(entry, "FAILED", null, check.failures().toString());
            throw new PreCheckFailedException(check.failures());
        }
        if (projects.findByAwemeIdAndAssetIdAndActiveFlag(cmd.awemeId(), cmd.assetId(), 1).isPresent()) {
            journal.finish(entry, "FAILED", null, "target conflict");
            throw new TargetConflictException("标的 aweme=" + cmd.awemeId() + " asset=" + cmd.assetId()
                    + " 已存在活跃全域项目");
        }
        if (!cmd.confirmStandardStop()) {
            journal.finish(entry, "FAILED", null, "confirmation required");
            throw new ConfirmationRequiredException(
                    "创建全域投放项目将停止同标的（aweme_id x asset_id）在投的标准投放项目，需显式确认 confirmStandardStop=true");
        }
        List<List<String>> chunks = chunk(cmd.videoIds(), properties.chunkSize());
        Map<String, Object> params = buildCreateParams(cmd, chunks.isEmpty() ? List.of() : chunks.get(0));
        MapiResult result;
        try {
            result = mapi.call(MapiEndpoint.UNI_PROJECT_CREATE, cmd.advertiserId(), params);
        } catch (MapiException e) {
            if (e.code() == -1) {
                Optional<JsonNode> adopted = adoptByTarget(cmd, entry.createdAt);
                if (adopted.isPresent()) {
                    UniProject project = saveProject(cmd, adopted.get().path("project_id").asLong(), requestId);
                    entry.projectLocalId = project.id;
                    journal.finish(entry, "ADOPTED", e.requestId(), "超时后先查后认领 project_id=" + project.projectId);
                    materials.recordBatch(project, chunks.isEmpty() ? List.of() : chunks.get(0), 1);
                    appendRemaining(project, chunks, requestId);
                    return project;
                }
            }
            journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
            throw e;
        }
        long projectId = result.data().path("project_id").asLong();
        UniProject project = saveProject(cmd, projectId, requestId);
        entry.projectLocalId = project.id;
        materials.recordBatch(project, chunks.isEmpty() ? List.of() : chunks.get(0), 1);
        JsonNode errorList = result.data().path("error_list");
        boolean partial = errorList.isArray() && !errorList.isEmpty();
        if (partial) {
            project.lastError = errorList.toString();
            projects.save(project);
            alerts.alert("WARN", "创建部分素材失败", "project_id=" + projectId + " error_list=" + errorList);
        }
        appendRemaining(project, chunks, requestId);
        journal.finish(entry, partial ? "PARTIAL" : "OK", result.requestId(), result.data().toString());
        return project;
    }

    @Transactional
    public UniProject update(long projectLocalId, UpdatePatch patch, String clientRequestId) {
        UniProject project = projects.findById(projectLocalId).orElseThrow();
        String requestId = clientRequestId == null ? UUID.randomUUID().toString() : clientRequestId;
        OpJournal entry = journal.begin(requestId, MapiEndpoint.UNI_PROJECT_UPDATE, project.advertiserId,
                project.id, "update project=" + project.projectId);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("advertiser_id", project.advertiserId);
        params.put("project_id", project.projectId);
        putIfPresent(params, "name", patch.name());
        putIfPresent(params, "budget", patch.budget());
        putIfPresent(params, "uni_roi_goal", patch.uniRoiGoal());
        putIfPresent(params, "monetization_mode", patch.monetizationMode());
        putIfPresent(params, "schedule_type", patch.scheduleType());
        putIfPresent(params, "end_time", patch.endTime());
        putIfPresent(params, "schedule_time", patch.scheduleTime());
        putIfPresent(params, "aigc_dynamic_creative_switch", patch.aigcDynamicCreativeSwitch());
        putIfPresent(params, "brand_info", patch.brandInfo());
        putIfPresent(params, "audience", patch.audience());
        if (params.size() == 2) {
            journal.finish(entry, "FAILED", null, "empty patch");
            throw new IllegalArgumentException("增量更新补丁为空：未传任何待更新字段");
        }
        MapiResult result;
        try {
            result = mapi.call(MapiEndpoint.UNI_PROJECT_UPDATE, project.advertiserId, params);
        } catch (MapiException e) {
            journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
            throw e;
        }
        JsonNode errorList = result.data().path("error_list");
        boolean partial = errorList.isArray() && !errorList.isEmpty();
        if (patch.budget() != null) {
            project.budget = patch.budget();
        }
        if (patch.uniRoiGoal() != null) {
            project.bid = patch.uniRoiGoal();
        }
        if (patch.monetizationMode() != null) {
            project.monetizationMode = patch.monetizationMode();
        }
        project.updatedAt = LocalDateTime.now();
        projects.save(project);
        if (partial) {
            alerts.alert("WARN", "项目分块更新部分失败", "project_id=" + project.projectId + " " + errorList);
        }
        journal.finish(entry, partial ? "PARTIAL" : "OK", result.requestId(), result.data().toString());
        return project;
    }

    /** 拉平台列表并同步本地状态/阶段；终态释放唯一标的占位（active_flag=NULL）。 */
    @Transactional
    public JsonNode syncFromRemote(UniProject project) {
        JsonNode data = mapi.call(MapiEndpoint.UNI_PROJECT_LIST, project.advertiserId,
                Map.of("advertiser_id", project.advertiserId,
                        "filtering", Map.of("project_id", List.of(project.projectId)),
                        "page", 1, "page_size", 10),
                JsonNode.class);
        JsonNode entry = data.path("list").isEmpty() ? null : data.path("list").get(0);
        if (entry == null) {
            return null;
        }
        String statusFirst = entry.path("status_first").asText();
        List<String> statusSecond = new ArrayList<>();
        entry.path("status_second").forEach(n -> statusSecond.add(n.asText()));
        project.status = statusFirst;
        boolean auditing = statusSecond.contains("PROJECT_STATUS_AUDIT")
                || statusSecond.contains("PROJECT_STATUS_REAUDIT");
        switch (statusFirst) {
            case "PROJECT_STATUS_ENABLE" -> project.stage = auditing ? UniProject.Stage.REVIEWING : UniProject.Stage.ACTIVE;
            case "PROJECT_STATUS_DISABLE" -> project.stage = auditing ? UniProject.Stage.REVIEWING : UniProject.Stage.PAUSED;
            case "PROJECT_STATUS_DELETE", "PROJECT_STATUS_FROZEN", "PROJECT_STATUS_DONE" -> {
                project.stage = UniProject.Stage.TERMINATED;
                project.activeFlag = null;
            }
            default -> log.warn("未知项目一级状态: {}", statusFirst);
        }
        if (entry.has("budget")) {
            project.budget = entry.path("budget").decimalValue();
        }
        if (entry.has("uni_roi_goal")) {
            project.bid = entry.path("uni_roi_goal").decimalValue();
        }
        if (entry.has("monetization_mode")) {
            project.monetizationMode = entry.path("monetization_mode").asText();
        }
        project.updatedAt = LocalDateTime.now();
        projects.save(project);
        return entry;
    }

    @Transactional
    public JsonNode rejectReasons(UniProject project) {
        JsonNode data = mapi.call(MapiEndpoint.OC_REJECT_REASON, project.advertiserId,
                Map.of("advertiser_id", project.advertiserId, "project_id", project.projectId,
                        "page", 1, "page_size", 100),
                JsonNode.class);
        project.rejectReasonJson = data.toString();
        project.updatedAt = LocalDateTime.now();
        projects.save(project);
        return data;
    }

    public Optional<UniProject> findByTarget(long awemeId, long assetId) {
        return projects.findByAwemeIdAndAssetIdAndActiveFlag(awemeId, assetId, 1);
    }

    private Map<String, Object> buildCreateParams(CreateProjectCommand cmd, List<String> firstChunk) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("advertiser_id", cmd.advertiserId());
        params.put("name", cmd.name());
        params.put("delivery_product", "ALBUM");
        params.put("related_product", Map.of(
                "product_platform_id", cmd.productPlatformId(),
                "product_id", cmd.assetId()));
        params.put("aweme_id", String.valueOf(cmd.awemeId()));
        params.put("budget", cmd.budget());
        params.put("uni_roi_goal", cmd.uniRoiGoal());
        params.put("monetization_mode", cmd.monetizationMode());
        params.put("schedule_type", cmd.scheduleType());
        params.put("source", cmd.source());
        putIfPresent(params, "start_time", cmd.startTime());
        putIfPresent(params, "end_time", cmd.endTime());
        putIfPresent(params, "schedule_time", cmd.scheduleTime());
        List<Map<String, Object>> videos = new ArrayList<>();
        for (String videoId : firstChunk) {
            Map<String, Object> video = new LinkedHashMap<>();
            video.put("image_mode", cmd.imageMode() == null ? DEFAULT_IMAGE_MODE : cmd.imageMode());
            video.put("video_id", videoId);
            videos.add(video);
        }
        if (!videos.isEmpty()) {
            params.put("video_material_list", videos);
        }
        Map<String, Object> brandInfo = cmd.brandInfoOverride();
        if (brandInfo == null) {
            String stored = advertisers.findById(cmd.advertiserId()).map(a -> a.brandInfoJson).orElse(null);
            if (stored != null && !stored.isBlank()) {
                brandInfo = parseMap(stored);
            }
        }
        putIfPresent(params, "brand_info", brandInfo);
        return params;
    }

    private UniProject saveProject(CreateProjectCommand cmd, long projectId, String requestId) {
        UniProject project = new UniProject();
        project.advertiserId = cmd.advertiserId();
        project.projectId = projectId;
        project.awemeId = cmd.awemeId();
        project.assetId = cmd.assetId();
        project.activeFlag = 1;
        project.stage = UniProject.Stage.REVIEWING;
        project.monetizationMode = cmd.monetizationMode();
        project.budget = cmd.budget();
        project.bid = cmd.uniRoiGoal();
        project.clientRequestId = requestId;
        project.confirmedConflict = cmd.confirmStandardStop();
        project.createdAt = LocalDateTime.now();
        project.updatedAt = LocalDateTime.now();
        return projects.save(project);
    }

    private void appendRemaining(UniProject project, List<List<String>> chunks, String requestId) {
        for (int i = 1; i < chunks.size(); i++) {
            materials.appendVideos(project, chunks.get(i), i + 1, requestId + "-m" + (i + 1));
        }
    }

    /** 创建超时（传输层错误重试耗尽）后先查后认领：按创建时间窗 + 标的匹配，杜绝重复项目。 */
    private Optional<JsonNode> adoptByTarget(CreateProjectCommand cmd, LocalDateTime since) {
        Map<String, Object> filtering = Map.of(
                "project_create_start_time", since.minusMinutes(2).format(TS),
                "project_create_end_time", LocalDateTime.now().plusMinutes(1).format(TS));
        for (int page = 1; page <= 10; page++) {
            JsonNode data;
            try {
                data = mapi.call(MapiEndpoint.UNI_PROJECT_LIST, cmd.advertiserId(),
                        Map.of("advertiser_id", cmd.advertiserId(), "filtering", filtering,
                                "page", page, "page_size", 100),
                        JsonNode.class);
            } catch (MapiException e) {
                log.error("认领查询失败: {}", e.getMessage());
                return Optional.empty();
            }
            for (JsonNode entry : data.path("list")) {
                if (String.valueOf(cmd.awemeId()).equals(entry.path("aweme_id").asText())
                        && entry.path("related_product").path("product_id").asLong() == cmd.assetId()) {
                    return Optional.of(entry);
                }
            }
            if (page >= data.path("page_info").path("total_page").asInt(1)) {
                break;
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> parseMap(String json) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("brand_info 反序列化失败", e);
        }
    }

    private static void putIfPresent(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    static <T> List<List<T>> chunk(List<T> items, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            chunks.add(items.subList(i, Math.min(items.size(), i + size)));
        }
        return chunks;
    }
}
