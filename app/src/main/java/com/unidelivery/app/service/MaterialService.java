package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.config.UniDeliveryProperties;
import com.unidelivery.app.domain.OpJournal;
import com.unidelivery.app.domain.ProjectMaterial;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.dto.BatchOutcome;
import com.unidelivery.app.repo.ProjectMaterialRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiException;
import com.unidelivery.mapi.MapiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目下素材：追投（单次总量 ≤200）、游标分页回读（material_status_frist 为平台原文拼写）、
 * 对账（本地 ↔ 平台）、删除（平台禁止删光）、启停（单批 ≤100，部分成功语义）。
 */
@Service
public class MaterialService {

    private static final Logger log = LoggerFactory.getLogger(MaterialService.class);
    private static final String DEFAULT_IMAGE_MODE = "CREATIVE_IMAGE_MODE_VIDEO_VERTICAL";
    private static final int STATUS_BATCH = 100;
    private static final int MAX_CURSOR_PAGES = 50;

    private final MapiClient mapi;
    private final ProjectMaterialRepository materials;
    private final JournalService journal;
    private final AlertService alerts;
    private final UniDeliveryProperties properties;

    public MaterialService(MapiClient mapi, ProjectMaterialRepository materials, JournalService journal,
                           AlertService alerts, UniDeliveryProperties properties) {
        this.mapi = mapi;
        this.materials = materials;
        this.journal = journal;
        this.alerts = alerts;
        this.properties = properties;
    }

    /** 创建时首批素材落本地（状态待回读核定）。 */
    @Transactional
    public void recordBatch(UniProject project, List<String> videoIds, int batchNo) {
        for (String videoId : videoIds) {
            if (materials.findByProjectLocalIdAndVideoId(project.id, videoId).isPresent()) {
                continue;
            }
            ProjectMaterial row = new ProjectMaterial();
            row.projectLocalId = project.id;
            row.videoId = videoId;
            row.status = "UNKNOWN";
            row.batchNo = batchNo;
            row.createdAt = LocalDateTime.now();
            row.updatedAt = LocalDateTime.now();
            materials.save(row);
        }
    }

    /** 追投：单次总素材量 ≤200（契约），超出自动分批；error_list 非空 = 部分成功。 */
    @Transactional
    public void appendVideos(UniProject project, List<String> videoIds, int batchNo, String clientRequestId) {
        List<List<String>> chunks = ProjectService.chunk(videoIds, properties.chunkSize());
        for (int i = 0; i < chunks.size(); i++) {
            String requestId = clientRequestId + "-" + (i + 1);
            OpJournal entry = journal.begin(requestId, MapiEndpoint.OC_MATERIAL_CREATE, project.advertiserId,
                    project.id, "append videos=" + chunks.get(i).size() + " batch=" + batchNo);
            List<Map<String, Object>> videos = new ArrayList<>();
            for (String videoId : chunks.get(i)) {
                videos.add(Map.of("image_mode", DEFAULT_IMAGE_MODE, "video_id", videoId));
            }
            MapiResult result;
            try {
                result = mapi.call(MapiEndpoint.OC_MATERIAL_CREATE, project.advertiserId,
                        Map.of("advertiser_id", project.advertiserId, "project_id", project.projectId,
                                "video_material_list", videos));
            } catch (MapiException e) {
                journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
                throw e;
            }
            recordBatch(project, chunks.get(i), batchNo);
            JsonNode errorList = result.data().path("error_list");
            boolean partial = errorList.isArray() && !errorList.isEmpty();
            if (partial) {
                alerts.alert("WARN", "追投部分素材失败",
                        "project_id=" + project.projectId + " error_list=" + errorList);
            }
            journal.finish(entry, partial ? "PARTIAL" : "OK", result.requestId(), result.data().toString());
        }
    }

    /** 游标分页回读项目下视频素材（cursor 优先于 page，契约）。 */
    public List<JsonNode> listVideos(UniProject project) {
        List<JsonNode> all = new ArrayList<>();
        long cursor = 0;
        for (int i = 0; i < MAX_CURSOR_PAGES; i++) {
            JsonNode data = mapi.call(MapiEndpoint.OC_MATERIAL_GET, project.advertiserId,
                    Map.of("advertiser_id", project.advertiserId, "project_id", project.projectId,
                            "filtering", Map.of("material_type", "VIDEO"),
                            "cursor", cursor, "count", 100),
                    JsonNode.class);
            data.path("video_material_list").forEach(all::add);
            JsonNode cursorInfo = data.path("cursor_info");
            if (!cursorInfo.path("has_more").asBoolean(false)) {
                break;
            }
            cursor = cursorInfo.path("cursor").asLong();
        }
        return all;
    }

    /** 对账：平台为真相源；本地多余标记已删除，缺失补录，状态/素材ID 刷新。返回差异数。 */
    @Transactional
    public int reconcile(UniProject project) {
        List<JsonNode> remote = listVideos(project);
        Map<String, JsonNode> remoteByVideo = new LinkedHashMap<>();
        for (JsonNode node : remote) {
            remoteByVideo.put(node.path("video_id").asText(), node);
        }
        int diffs = 0;
        List<ProjectMaterial> local = materials.findByProjectLocalId(project.id);
        for (ProjectMaterial row : local) {
            JsonNode node = remoteByVideo.remove(row.videoId);
            if (node == null) {
                if (!"STATUS_DELETE".equals(row.status)) {
                    row.status = "STATUS_DELETE";
                    row.updatedAt = LocalDateTime.now();
                    materials.save(row);
                    diffs++;
                }
                continue;
            }
            String status = node.path("material_status_frist").asText();
            Long materialId = node.path("material_id").asLong();
            String optStatus = node.path("material_opt_status").asText();
            if (!status.equals(row.status) || !materialId.equals(row.materialId)
                    || !optStatus.equals(row.optStatus)) {
                row.status = status;
                row.materialId = materialId;
                row.optStatus = optStatus;
                row.updatedAt = LocalDateTime.now();
                materials.save(row);
                diffs++;
            }
        }
        for (Map.Entry<String, JsonNode> entry : remoteByVideo.entrySet()) {
            ProjectMaterial row = new ProjectMaterial();
            row.projectLocalId = project.id;
            row.videoId = entry.getKey();
            row.materialId = entry.getValue().path("material_id").asLong();
            row.status = entry.getValue().path("material_status_frist").asText();
            row.optStatus = entry.getValue().path("material_opt_status").asText();
            row.batchNo = 0;
            row.createdAt = LocalDateTime.now();
            row.updatedAt = LocalDateTime.now();
            materials.save(row);
            diffs++;
        }
        return diffs;
    }

    /** 删除：平台不支持删光项目下素材（剩 1 个再删报错），本地先闸门拦截。 */
    @Transactional
    public void delete(UniProject project, List<Long> materialIds, String clientRequestId) {
        long activeRemote = listVideos(project).stream()
                .filter(n -> !"STATUS_DELETE".equals(n.path("material_status_frist").asText()))
                .count();
        if (materialIds.size() >= activeRemote) {
            throw new IllegalArgumentException("平台不允许删光项目下素材：在投 " + activeRemote
                    + " 个，请求删除 " + materialIds.size() + " 个");
        }
        OpJournal entry = journal.begin(clientRequestId, MapiEndpoint.OC_MATERIAL_DELETE, project.advertiserId,
                project.id, "delete materials=" + materialIds);
        MapiResult result;
        try {
            result = mapi.call(MapiEndpoint.OC_MATERIAL_DELETE, project.advertiserId,
                    Map.of("advertiser_id", project.advertiserId, "project_id", project.projectId,
                            "material_id", materialIds));
        } catch (MapiException e) {
            journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
            throw e;
        }
        for (ProjectMaterial row : materials.findByProjectLocalId(project.id)) {
            if (row.materialId != null && materialIds.contains(row.materialId)) {
                row.status = "STATUS_DELETE";
                row.updatedAt = LocalDateTime.now();
                materials.save(row);
            }
        }
        journal.finish(entry, "OK", result.requestId(), result.data().toString());
    }

    /** 启停：单批 1~100（契约）；errors 非空 = 部分成功。 */
    @Transactional
    public BatchOutcome setStatus(UniProject project, Map<Long, String> optStatusByMaterial,
                                  String clientRequestId) {
        List<Long> ids = new ArrayList<>(optStatusByMaterial.keySet());
        List<Long> success = new ArrayList<>();
        List<BatchOutcome.ErrorEntry> errors = new ArrayList<>();
        List<List<Long>> chunks = ProjectService.chunk(ids, STATUS_BATCH);
        for (int i = 0; i < chunks.size(); i++) {
            String requestId = clientRequestId + "-" + (i + 1);
            OpJournal entry = journal.begin(requestId, MapiEndpoint.OC_MATERIAL_STATUS_UPDATE,
                    project.advertiserId, project.id, "status update materials=" + chunks.get(i).size());
            List<Map<String, Object>> data = new ArrayList<>();
            for (Long materialId : chunks.get(i)) {
                String opt = optStatusByMaterial.get(materialId);
                if (!"ENABLE".equals(opt) && !"DISABLE".equals(opt)) {
                    throw new IllegalArgumentException("opt_status 仅允许 ENABLE/DISABLE: " + opt);
                }
                data.add(Map.of("material_id", materialId, "opt_status", opt));
            }
            MapiResult result;
            try {
                result = mapi.call(MapiEndpoint.OC_MATERIAL_STATUS_UPDATE, project.advertiserId,
                        Map.of("advertiser_id", project.advertiserId, "project_id", project.projectId,
                                "data", data));
            } catch (MapiException e) {
                journal.finish(entry, "FAILED", e.requestId(), e.getMessage());
                throw e;
            }
            result.data().path("material_ids").forEach(n -> success.add(n.asLong()));
            result.data().path("errors").forEach(n ->
                    errors.add(new BatchOutcome.ErrorEntry(n.path("material_id").asLong(),
                            n.path("message").asText())));
            boolean partial = !errors.isEmpty();
            journal.finish(entry, partial ? "PARTIAL" : "OK", result.requestId(), result.data().toString());
        }
        for (ProjectMaterial row : materials.findByProjectLocalId(project.id)) {
            if (row.materialId != null && success.contains(row.materialId)) {
                row.optStatus = optStatusByMaterial.get(row.materialId);
                row.updatedAt = LocalDateTime.now();
                materials.save(row);
            }
        }
        if (!errors.isEmpty()) {
            alerts.alert("WARN", "素材启停部分失败", "project_id=" + project.projectId + " errors=" + errors);
        }
        return new BatchOutcome(success, errors);
    }
}
