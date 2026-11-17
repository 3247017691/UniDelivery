package com.unidelivery.app.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.repo.OpJournalRepository;
import com.unidelivery.app.repo.ProjectMaterialRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.app.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 端到端：预检 → 确认闸门 → 200 分批创建 → 幂等重放 → 部分成功语义。 */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:e2e_create;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class E2ECreateTest extends MapiStubBase {

    @Autowired
    private UniProjectRepository projects;
    @Autowired
    private ProjectMaterialRepository materials;
    @Autowired
    private OpJournalRepository journals;
    @Autowired
    private ProductSyncService productSync;

    private Map<String, Object> command(long awemeId, long assetId, boolean confirm, String requestId,
                                        int videoCount) {
        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("advertiserId", 1001);
        cmd.put("awemeId", awemeId);
        cmd.put("assetId", assetId);
        cmd.put("productPlatformId", 6001);
        cmd.put("playletId", 9001);
        cmd.put("name", "e2e-短剧项目");
        cmd.put("budget", 300);
        cmd.put("uniRoiGoal", 1.5);
        cmd.put("monetizationMode", "IAP");
        cmd.put("scheduleType", "SCHEDULE_FROM_NOW");
        cmd.put("source", "e2e剧场");
        cmd.put("videoIds", IntStream.range(0, videoCount).mapToObj(i -> "v" + i).toList());
        cmd.put("confirmStandardStop", confirm);
        cmd.put("clientRequestId", requestId);
        return cmd;
    }

    private void seedProduct(long assetId) {
        productSync.record(1001, assetId, 5001, 6001, null, null);
        stubProduct(assetId, "9001", 1, 1);
        ProductSyncService.SyncOutcome outcome = productSync.sync(1001, assetId);
        assertTrue(outcome.ok(), outcome.reason());
    }

    @Test
    void fullCreateWith200ChunkingAndIdempotentReplay() throws Exception {
        seedAdvertiser(1001);
        stubAuth(List.of("7001"));
        stubAuthorized(List.of("7001"));
        stubBrandEmpty();
        seedProduct(3001);
        stubCreate(555, "[]");
        stubMaterialCreate("[]");

        ResponseEntity<JsonNode> pre = rest.postForEntity("/api/precheck",
                Map.of("advertiserId", 1001, "awemeId", 7001, "assetId", 3001, "playletId", 9001),
                JsonNode.class);
        assertEquals(200, pre.getStatusCode().value());
        assertTrue(pre.getBody().path("allPassed").asBoolean());

        ResponseEntity<UniProject> created = rest.postForEntity("/api/projects",
                command(7001, 3001, true, "e2e-create-1", 250), UniProject.class);
        assertEquals(200, created.getStatusCode().value());
        UniProject project = created.getBody();
        assertNotNull(project);
        assertEquals(555L, project.projectId);
        assertEquals(UniProject.Stage.REVIEWING, project.stage);
        assertEquals(250, materials.findByProjectLocalId(project.id).size());

        // 契约闸门：create 首批 200，追投批 50
        var createRequests = MAPI.findAll(postRequestedFor(urlPathEqualTo("/open_api/v3.0/uni_project/create/")));
        assertEquals(1, createRequests.size());
        JsonNode createBody = JSON.readTree(createRequests.get(0).getBodyAsString());
        assertEquals(200, createBody.path("video_material_list").size());
        assertEquals("ALBUM", createBody.path("delivery_product").asText());
        assertEquals("7001", createBody.path("aweme_id").asText());
        var appendRequests = MAPI.findAll(postRequestedFor(urlPathEqualTo("/open_api/v3.0/oc_project/material/create/")));
        assertEquals(1, appendRequests.size());
        assertEquals(50, JSON.readTree(appendRequests.get(0).getBodyAsString())
                .path("video_material_list").size());

        assertEquals("OK", journals.findByClientRequestId("e2e-create-1").orElseThrow().status);
        assertEquals("OK", journals.findByClientRequestId("e2e-create-1-m2-1").orElseThrow().status);

        // 幂等重放：同 clientRequestId 不再触发平台创建
        ResponseEntity<UniProject> replay = rest.postForEntity("/api/projects",
                command(7001, 3001, true, "e2e-create-1", 250), UniProject.class);
        assertEquals(200, replay.getStatusCode().value());
        assertEquals(project.id, replay.getBody().id);
        assertEquals(1, MAPI.findAll(postRequestedFor(
                urlPathEqualTo("/open_api/v3.0/uni_project/create/"))).size());
    }

    @Test
    void confirmationGateThenTargetConflict() {
        seedAdvertiser(1001);
        stubAuth(List.of("7001", "7002"));
        stubAuthorized(List.of("7001", "7002"));
        stubBrandEmpty();
        seedProduct(3002);
        stubCreate(556, "[]");
        stubMaterialCreate("[]");

        ResponseEntity<JsonNode> noConfirm = rest.postForEntity("/api/projects",
                command(7002, 3002, false, "e2e-create-2", 1), JsonNode.class);
        assertEquals(428, noConfirm.getStatusCode().value());
        assertEquals("CONFIRMATION_REQUIRED", noConfirm.getBody().path("error").asText());

        ResponseEntity<UniProject> confirmed = rest.postForEntity("/api/projects",
                command(7002, 3002, true, "e2e-create-3", 1), UniProject.class);
        assertEquals(200, confirmed.getStatusCode().value());

        // 标的冲突在预检闸门即拦截（TARGET 项 fail-closed）
        ResponseEntity<JsonNode> conflict = rest.postForEntity("/api/projects",
                command(7002, 3002, true, "e2e-create-4", 1), JsonNode.class);
        assertEquals(422, conflict.getStatusCode().value());
        assertTrue(conflict.getBody().path("failures").toString().contains("TARGET"));
    }

    @Test
    void precheckFailBlocksCreate() {
        // 账户 1002 未录入白名单 → 预检 fail-closed
        stubAuth(List.of("7001"));
        stubAuthorized(List.of("7001"));
        stubBrandEmpty();
        ResponseEntity<JsonNode> pre = rest.postForEntity("/api/precheck",
                Map.of("advertiserId", 1002, "awemeId", 7001, "assetId", 3001, "playletId", 9001),
                JsonNode.class);
        assertEquals(200, pre.getStatusCode().value());
        assertFalse(pre.getBody().path("allPassed").asBoolean());

        Map<String, Object> cmd = command(7001, 3001, true, "e2e-create-5", 1);
        cmd.put("advertiserId", 1002);
        ResponseEntity<JsonNode> blocked = rest.postForEntity("/api/projects", cmd, JsonNode.class);
        assertEquals(422, blocked.getStatusCode().value());
        assertEquals("PRECHECK_FAILED", blocked.getBody().path("error").asText());
        assertTrue(blocked.getBody().path("failures").toString().contains("WHITELIST"));
        assertEquals(0, MAPI.findAll(postRequestedFor(
                urlPathEqualTo("/open_api/v3.0/uni_project/create/"))).size());
    }

    @Test
    void partialCreateIsNotFullSuccess() {
        seedAdvertiser(1001);
        stubAuth(List.of("7003"));
        stubAuthorized(List.of("7003"));
        stubBrandEmpty();
        seedProduct(3003);
        stubCreate(557, "[{\"object_type\":\"VIDEO\",\"error_code\":\"40000\",\"error_message\":\"素材审核失败\"}]");
        stubMaterialCreate("[]");

        ResponseEntity<UniProject> created = rest.postForEntity("/api/projects",
                command(7003, 3003, true, "e2e-create-6", 1), UniProject.class);
        assertEquals(200, created.getStatusCode().value());
        assertEquals("PARTIAL", journals.findByClientRequestId("e2e-create-6").orElseThrow().status);
        assertNotNull(projects.findById(created.getBody().id).orElseThrow().lastError);
    }
}
