package com.unidelivery.app.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.Advertiser;
import com.unidelivery.app.domain.UniProject;
import com.unidelivery.app.dto.BatchOutcome;
import com.unidelivery.app.dto.CreateProjectCommand;
import com.unidelivery.app.dto.PreCheckResult;
import com.unidelivery.app.dto.UpdatePatch;
import com.unidelivery.app.repo.AdvertiserRepository;
import com.unidelivery.app.repo.UniProjectRepository;
import com.unidelivery.app.service.CommentService;
import com.unidelivery.app.service.DeliveryService;
import com.unidelivery.app.service.MaterialService;
import com.unidelivery.app.service.OpLogService;
import com.unidelivery.app.service.PreCheckService;
import com.unidelivery.app.service.ProductSyncService;
import com.unidelivery.app.service.ProjectService;
import com.unidelivery.app.service.ReportService;
import com.unidelivery.app.service.TokenService;
import com.unidelivery.app.service.UploadService;
import com.unidelivery.mapi.MapiEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 运营/集成 REST 面：所有写操作经服务层闸门与 journal；读操作直连平台或本地投影。
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProjectService projects;
    private final MaterialService materials;
    private final DeliveryService delivery;
    private final PreCheckService preCheck;
    private final ProductSyncService products;
    private final UploadService uploads;
    private final ReportService reports;
    private final OpLogService opLogs;
    private final CommentService comments;
    private final TokenService tokens;
    private final UniProjectRepository projectRepository;
    private final AdvertiserRepository advertiserRepository;

    public AdminController(ProjectService projects, MaterialService materials, DeliveryService delivery,
                           PreCheckService preCheck, ProductSyncService products, UploadService uploads,
                           ReportService reports, OpLogService opLogs, CommentService comments,
                           TokenService tokens, UniProjectRepository projectRepository,
                           AdvertiserRepository advertiserRepository) {
        this.projects = projects;
        this.materials = materials;
        this.delivery = delivery;
        this.preCheck = preCheck;
        this.products = products;
        this.uploads = uploads;
        this.reports = reports;
        this.opLogs = opLogs;
        this.comments = comments;
        this.tokens = tokens;
        this.projectRepository = projectRepository;
        this.advertiserRepository = advertiserRepository;
    }

    public record WhitelistRequest(long advertiserId, String name, String evidence) {
    }

    @PostMapping("/admin/whitelist")
    public Advertiser whitelist(@RequestBody WhitelistRequest request) {
        Advertiser advertiser = advertiserRepository.findById(request.advertiserId())
                .orElseGet(() -> {
                    Advertiser created = new Advertiser();
                    created.advertiserId = request.advertiserId();
                    created.createdAt = LocalDateTime.now();
                    return created;
                });
        advertiser.name = request.name();
        advertiser.whitelisted = true;
        advertiser.whitelistEvidence = request.evidence();
        advertiser.updatedAt = LocalDateTime.now();
        return advertiserRepository.save(advertiser);
    }

    @GetMapping("/oauth/authorize-url")
    public Map<String, String> authorizeUrl(@RequestParam String redirectUri, @RequestParam String state) {
        return Map.of("url", tokens.authorizeUrl(redirectUri, state));
    }

    public record ExchangeRequest(long advertiserId, String authCode) {
    }

    @PostMapping("/oauth/exchange")
    public Map<String, Object> exchange(@RequestBody ExchangeRequest request) {
        tokens.exchangeAuthCode(request.advertiserId(), request.authCode());
        return Map.of("ok", true);
    }

    public record PreCheckRequest(long advertiserId, long awemeId, long assetId, Long playletId) {
    }

    @PostMapping("/precheck")
    public PreCheckResult precheck(@RequestBody PreCheckRequest request) {
        return preCheck.run(request.advertiserId(), request.awemeId(), request.assetId(), request.playletId());
    }

    @PostMapping("/projects")
    public UniProject create(@RequestBody CreateProjectCommand command) {
        return projects.create(command);
    }

    @GetMapping("/projects")
    public List<UniProject> list(@RequestParam long advertiserId) {
        return projectRepository.findByAdvertiserId(advertiserId);
    }

    public record UpdateRequest(UpdatePatch patch, String clientRequestId) {
    }

    @PostMapping("/projects/{id}/update")
    public UniProject update(@PathVariable long id, @RequestBody UpdateRequest request) {
        return projects.update(id, request.patch(), request.clientRequestId());
    }

    @PostMapping("/projects/{id}/sync")
    public JsonNode sync(@PathVariable long id) {
        return projects.syncFromRemote(projectRepository.findById(id).orElseThrow());
    }

    @GetMapping("/projects/{id}/reject-reasons")
    public JsonNode rejectReasons(@PathVariable long id) {
        return projects.rejectReasons(projectRepository.findById(id).orElseThrow());
    }

    public record AppendRequest(List<String> videoIds, Integer batchNo, String clientRequestId) {
    }

    @PostMapping("/projects/{id}/materials/append")
    public ResponseEntity<Map<String, Object>> append(@PathVariable long id, @RequestBody AppendRequest request) {
        UniProject project = projectRepository.findById(id).orElseThrow();
        materials.appendVideos(project, request.videoIds(),
                request.batchNo() == null ? 2 : request.batchNo(),
                request.clientRequestId() == null ? java.util.UUID.randomUUID().toString()
                        : request.clientRequestId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/projects/{id}/materials/reconcile")
    public Map<String, Object> reconcileMaterials(@PathVariable long id) {
        int diffs = materials.reconcile(projectRepository.findById(id).orElseThrow());
        return Map.of("diffs", diffs);
    }

    public record DeleteMaterialsRequest(List<Long> materialIds, String clientRequestId) {
    }

    @PostMapping("/projects/{id}/materials/delete")
    public ResponseEntity<Map<String, Object>> deleteMaterials(@PathVariable long id,
                                                               @RequestBody DeleteMaterialsRequest request) {
        materials.delete(projectRepository.findById(id).orElseThrow(), request.materialIds(),
                request.clientRequestId() == null ? java.util.UUID.randomUUID().toString()
                        : request.clientRequestId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    public record MaterialStatusRequest(Map<Long, String> optStatusByMaterial, String clientRequestId) {
    }

    @PostMapping("/projects/{id}/materials/status")
    public BatchOutcome materialStatus(@PathVariable long id, @RequestBody MaterialStatusRequest request) {
        return materials.setStatus(projectRepository.findById(id).orElseThrow(),
                request.optStatusByMaterial(),
                request.clientRequestId() == null ? java.util.UUID.randomUUID().toString()
                        : request.clientRequestId());
    }

    public record StatusRequest(long advertiserId, List<Long> projectIds, String optStatus,
                                String clientRequestId) {
    }

    @PostMapping("/delivery/status")
    public BatchOutcome status(@RequestBody StatusRequest request) {
        return delivery.updateStatus(request.advertiserId(), request.projectIds(), request.optStatus(),
                request.clientRequestId());
    }

    public record RoiRequest(long advertiserId, Map<Long, BigDecimal> roiByProject, String clientRequestId) {
    }

    @PostMapping("/delivery/roi")
    public BatchOutcome roi(@RequestBody RoiRequest request) {
        return delivery.updateRoiGoal(request.advertiserId(), request.roiByProject(), request.clientRequestId());
    }

    public record MonetizationRequest(long advertiserId, List<DeliveryService.MonetizationChange> changes,
                                      String clientRequestId) {
    }

    @PostMapping("/delivery/monetization")
    public BatchOutcome monetization(@RequestBody MonetizationRequest request) {
        return delivery.updateMonetization(request.advertiserId(), request.changes(), request.clientRequestId());
    }

    public record PassthroughRequest(String endpoint, long advertiserId, Map<String, Object> payload,
                                     String clientRequestId, Long projectLocalId) {
    }

    @PostMapping("/delivery/passthrough")
    public JsonNode passthrough(@RequestBody PassthroughRequest request) {
        return delivery.auditedPassthrough(MapiEndpoint.valueOf(request.endpoint()), request.advertiserId(),
                request.payload(), request.clientRequestId(), request.projectLocalId());
    }

    public record ProductRecordRequest(long advertiserId, long assetId, long orgId, long platformId,
                                       Long playletId, Long albumId) {
    }

    @PostMapping("/products/record")
    public Object recordProduct(@RequestBody ProductRecordRequest request) {
        return products.record(request.advertiserId(), request.assetId(), request.orgId(),
                request.platformId(), request.playletId(), request.albumId());
    }

    public record ProductSyncRequest(long advertiserId, long assetId) {
    }

    @PostMapping("/products/sync")
    public ProductSyncService.SyncOutcome syncProduct(@RequestBody ProductSyncRequest request) {
        return products.sync(request.advertiserId(), request.assetId());
    }

    @PostMapping("/uploads/file")
    public Map<String, String> uploadFile(@RequestParam long advertiserId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) List<String> labels) throws Exception {
        Path temp = Files.createTempFile("uni-upload-", "-" + file.getOriginalFilename());
        try {
            file.transferTo(temp);
            String videoId = uploads.uploadFile(advertiserId, temp, labels);
            return Map.of("video_id", videoId);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public record UploadUrlRequest(long advertiserId, String tosUrl, String filename, List<String> labels) {
    }

    @PostMapping("/uploads/url")
    public Object uploadUrl(@RequestBody UploadUrlRequest request) {
        return uploads.uploadByUrl(request.advertiserId(), request.tosUrl(), request.filename(), request.labels());
    }

    @PostMapping("/uploads/poll")
    public Map<String, Object> pollUploads() {
        return Map.of("settled", uploads.pollTasks());
    }

    @GetMapping("/uploads/labels")
    public Map<String, Object> labels(@RequestParam long advertiserId, @RequestParam String videoId,
                                      @RequestParam(required = false) String required) {
        List<String> actual = uploads.readLabels(advertiserId, videoId);
        return Map.of("labels", actual,
                "has_required", required == null || actual.contains(required));
    }

    public record ReportPullRequest(long advertiserId, String topic, List<String> dimensions,
                                    List<String> metrics, String start, String end, String channel) {
    }

    @PostMapping("/reports/pull")
    public Map<String, Object> pullReport(@RequestBody ReportPullRequest request) {
        int rows = reports.pull(request.advertiserId(), request.topic(), request.dimensions(),
                request.metrics(), LocalDateTime.parse(request.start(), TS),
                LocalDateTime.parse(request.end(), TS),
                request.channel() == null ? "OFFLINE" : request.channel(), null);
        return Map.of("rows", rows);
    }

    @GetMapping("/reports/config")
    public JsonNode reportConfig(@RequestParam long advertiserId, @RequestParam List<String> topics) {
        return reports.config(advertiserId, topics);
    }

    public record ReportReconcileRequest(long advertiserId, String topic, String day) {
    }

    @PostMapping("/reports/reconcile")
    public Map<String, Object> reconcileReport(@RequestBody ReportReconcileRequest request) {
        List<String> breaches = reports.reconcile(request.advertiserId(), request.topic(),
                LocalDateTime.parse(request.day() + " 00:00:00", TS));
        return Map.of("breaches", breaches);
    }

    public record OpLogRequest(long advertiserId, String since, String until) {
    }

    @PostMapping("/oplogs/sync")
    public Map<String, Object> syncOpLogs(@RequestBody OpLogRequest request) {
        int synced = opLogs.sync(request.advertiserId(), LocalDateTime.parse(request.since(), TS),
                LocalDateTime.parse(request.until(), TS));
        return Map.of("synced", synced);
    }

    @PostMapping("/oplogs/reconcile")
    public Map<String, Object> reconcileOpLogs(@RequestBody OpLogRequest request) {
        List<String> untracked = opLogs.reconcile(request.advertiserId(),
                LocalDateTime.parse(request.since(), TS), LocalDateTime.parse(request.until(), TS));
        return Map.of("untracked", untracked);
    }

    public record CommentRequest(long advertiserId, Map<String, Object> params, String clientRequestId) {
    }

    @PostMapping("/comments/list")
    public JsonNode commentList(@RequestBody CommentRequest request) {
        return comments.list(request.advertiserId(), request.params());
    }

    @PostMapping("/comments/reply")
    public JsonNode commentReply(@RequestBody CommentRequest request) {
        return comments.reply(request.advertiserId(), request.params(), request.clientRequestId());
    }

    @PostMapping("/comments/hide")
    public JsonNode commentHide(@RequestBody CommentRequest request) {
        return comments.hide(request.advertiserId(), request.params(), request.clientRequestId());
    }

    @PostMapping("/comments/metrics")
    public JsonNode commentMetrics(@RequestBody CommentRequest request) {
        return comments.metrics(request.advertiserId(), request.params());
    }
}
