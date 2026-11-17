package com.unidelivery.app.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.ProjectMaterial;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.repo.OpJournalRepository;
import com.unidelivery.app.repo.PlatformLogRepository;
import com.unidelivery.app.repo.ProjectMaterialRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 端到端：增量编辑、调控批量、素材对账/删光闸门/部分成功、大日志审计对账、变现模式。 */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:e2e_ops;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class E2EOperationsTest extends MapiStubBase {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UniProjectRepository projects;
    @Autowired
    private ProjectMaterialRepository materials;
    @Autowired
    private OpJournalRepository journals;
    @Autowired
    private PlatformLogRepository platformLogs;

    private UniProject project;

    @BeforeEach
    void seedProject() {
        seedAdvertiser(1001);
        project = projects.findByClientRequestId("seed-op-1").orElseGet(() -> {
            UniProject created = new UniProject();
            created.advertiserId = 1001L;
            created.projectId = 555L;
            created.awemeId = 7001L;
            created.assetId = 3001L;
            created.activeFlag = 1;
            created.stage = UniProject.Stage.ACTIVE;
            created.clientRequestId = "seed-op-1";
            created.createdAt = LocalDateTime.now();
            created.updatedAt = LocalDateTime.now();
            return projects.save(created);
        });
        if (materials.findByProjectLocalId(project.id).isEmpty()) {
            for (long materialId : new long[]{8, 9}) {
                ProjectMaterial row = new ProjectMaterial();
                row.projectLocalId = project.id;
                row.videoId = "v" + (materialId - 8);
                row.materialId = materialId;
                row.status = "STATUS_OK";
                row.optStatus = "ENABLE";
                row.batchNo = 1;
                row.createdAt = LocalDateTime.now();
                row.updatedAt = LocalDateTime.now();
                materials.save(row);
            }
        }
    }

    @Test
    void incrementalUpdateAndEmptyPatchRejected() {
        stubUpdate("[]");
        ResponseEntity<UniProject> updated = rest.postForEntity("/api/projects/" + project.id + "/update",
                Map.of("patch", Map.of("budget", 500), "clientRequestId", "op-upd-1"), UniProject.class);
        assertEquals(200, updated.getStatusCode().value());
        assertEquals(0, updated.getBody().budget.compareTo(new java.math.BigDecimal("500")));
        assertEquals("OK", journals.findByClientRequestId("op-upd-1").orElseThrow().status);

        ResponseEntity<JsonNode> empty = rest.postForEntity("/api/projects/" + project.id + "/update",
                Map.of("patch", Map.of(), "clientRequestId", "op-upd-2"), JsonNode.class);
        assertEquals(400, empty.getStatusCode().value());
    }

    @Test
    void deliveryStatusBatchAndRoiValidation() {
        stubProjectStatus("[555]", "[]");
        ResponseEntity<JsonNode> status = rest.postForEntity("/api/delivery/status",
                Map.of("advertiserId", 1001, "projectIds", List.of(555), "optStatus", "DISABLE",
                        "clientRequestId", "op-st-1"), JsonNode.class);
        assertEquals(200, status.getStatusCode().value());
        assertTrue(status.getBody().path("fullSuccess").asBoolean());

        ResponseEntity<JsonNode> badRoi = rest.postForEntity("/api/delivery/roi",
                Map.of("advertiserId", 1001, "roiByProject", Map.of("555", 6)), JsonNode.class);
        assertEquals(400, badRoi.getStatusCode().value());
    }

    @Test
    void deleteAllMaterialsBlockedByPlatformRule() {
        stubMaterialGet("[{\"video_id\":\"v0\",\"material_id\":8,"
                + "\"material_status_frist\":\"STATUS_OK\",\"material_opt_status\":\"ENABLE\"}]");
        ResponseEntity<JsonNode> blocked = rest.postForEntity(
                "/api/projects/" + project.id + "/materials/delete",
                Map.of("materialIds", List.of(8), "clientRequestId", "op-del-1"), JsonNode.class);
        assertEquals(400, blocked.getStatusCode().value());
        assertTrue(blocked.getBody().path("message").asText().contains("删光"));
    }

    @Test
    void materialStatusPartialSuccessIsRecorded() {
        stubMaterialStatus("[8]", "[{\"material_id\":9,\"message\":\"素材不存在\"}]");
        ResponseEntity<JsonNode> outcome = rest.postForEntity(
                "/api/projects/" + project.id + "/materials/status",
                Map.of("optStatusByMaterial", Map.of("8", "DISABLE", "9", "DISABLE"),
                        "clientRequestId", "op-ms-1"), JsonNode.class);
        assertEquals(200, outcome.getStatusCode().value());
        assertFalse(outcome.getBody().path("fullSuccess").asBoolean());
        assertEquals(1, outcome.getBody().path("errors").size());
        assertEquals("PARTIAL", journals.findByClientRequestId("op-ms-1-1").orElseThrow().status);
    }

    @Test
    void reconcileUsesPlatformAsTruth() {
        // 归一化本地行，消除同类其他测试（启停部分成功）留下的状态干扰
        for (ProjectMaterial row : materials.findByProjectLocalId(project.id)) {
            row.status = "STATUS_OK";
            row.optStatus = "ENABLE";
            materials.save(row);
        }
        stubMaterialGet("[{\"video_id\":\"v0\",\"material_id\":8,"
                + "\"material_status_frist\":\"STATUS_OK\",\"material_opt_status\":\"ENABLE\"},"
                + "{\"video_id\":\"v9\",\"material_id\":10,"
                + "\"material_status_frist\":\"STATUS_DISABLE\",\"material_opt_status\":\"DISABLE\"}]");
        ResponseEntity<JsonNode> reconciled = rest.postForEntity(
                "/api/projects/" + project.id + "/materials/reconcile", Map.of(), JsonNode.class);
        assertEquals(200, reconciled.getStatusCode().value());
        assertEquals(2, reconciled.getBody().path("diffs").asInt());
        List<ProjectMaterial> rows = materials.findByProjectLocalId(project.id);
        assertEquals("STATUS_DELETE", rows.stream().filter(r -> r.videoId.equals("v1"))
                .findFirst().orElseThrow().status);
        assertTrue(rows.stream().anyMatch(r -> r.videoId.equals("v9") && r.materialId == 10L));
    }

    @Test
    void opLogSyncExpandsBigLogAndReconcileFindsUntracked() {
        // 远离本类其他测试产生的 journal 时间窗，保证「未追溯」判定确定
        LocalDateTime opTime = LocalDateTime.now().minusHours(3);
        stubGet("/open_api/2/tools/log_search/",
                "{\"logs\":[{\"object_id\":555,\"object_type\":\"PROJECT\",\"content_title\":\"修改预算\","
                        + "\"create_time\":\"" + opTime.format(TS) + "\",\"second_log_id\":77,"
                        + "\"operator\":\"ops\",\"opt_ip\":\"1.1.1.1\",\"content_log\":[\"300\",\"500\"]}],"
                        + "\"page_info\":{\"total_page\":1}}");
        stubGet("/open_api/2/tools/log_search/detail/get/",
                "{\"log\":{\"content_title\":\"修改预算\",\"object_id\":555,\"content_log\":[\"300\",\"500\"]}}");
        ResponseEntity<JsonNode> synced = rest.postForEntity("/api/oplogs/sync",
                Map.of("advertiserId", 1001, "since", LocalDateTime.now().minusHours(4).format(TS),
                        "until", LocalDateTime.now().format(TS)), JsonNode.class);
        assertEquals(200, synced.getStatusCode().value());
        assertEquals(1, synced.getBody().path("synced").asInt());
        assertTrue(platformLogs.findByAdvertiserIdAndOpTimeBetween(1001L,
                        LocalDateTime.now().minusHours(4), LocalDateTime.now()).get(0)
                .detailJson.contains("修改预算"));

        ResponseEntity<JsonNode> untracked = rest.postForEntity("/api/oplogs/reconcile",
                Map.of("advertiserId", 1001, "since", LocalDateTime.now().minusHours(4).format(TS),
                        "until", LocalDateTime.now().format(TS)), JsonNode.class);
        assertEquals(1, untracked.getBody().path("untracked").size());
    }

    @Test
    void monetizationRequiresIaaUrlForMixedMode() {
        ResponseEntity<JsonNode> missing = rest.postForEntity("/api/delivery/monetization",
                Map.of("advertiserId", 1001, "changes",
                        List.of(Map.of("projectId", 555, "mode", "IAA_AND_IAP"))), JsonNode.class);
        assertEquals(400, missing.getStatusCode().value());

        stubMonetization("[555]", "[]");
        ResponseEntity<JsonNode> ok = rest.postForEntity("/api/delivery/monetization",
                Map.of("advertiserId", 1001, "changes",
                        List.of(Map.of("projectId", 555, "mode", "IAA_AND_IAP",
                                "iaaUrl", "https://tos.example/iaa-link")),
                        "clientRequestId", "op-mo-1"), JsonNode.class);
        assertEquals(200, ok.getStatusCode().value());
        assertTrue(ok.getBody().path("fullSuccess").asBoolean());
        assertEquals("IAA_AND_IAP", projects.findById(project.id).orElseThrow().monetizationMode);
    }
}
