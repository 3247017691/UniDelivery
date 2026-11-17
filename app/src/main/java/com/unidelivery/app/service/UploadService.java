package com.unidelivery.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unidelivery.app.domain.UploadTask;
import com.unidelivery.app.repo.UploadTaskRepository;
import com.unidelivery.mapi.MapiClient;
import com.unidelivery.mapi.MapiEndpoint;
import com.unidelivery.mapi.MapiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频上传流水线：MD5 去重 → 同步 multipart（≤500M）或异步 tos 链接任务（≤1000M）→
 * 轮询 PROCESS/SUCCESS/FAILED → 「仅IAA」标签回读校验（R7）。
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private static final int TASK_BATCH = 100;

    private final MapiClient mapi;
    private final UploadTaskRepository tasks;
    private final AlertService alerts;

    public UploadService(MapiClient mapi, UploadTaskRepository tasks, AlertService alerts) {
        this.mapi = mapi;
        this.tasks = tasks;
        this.alerts = alerts;
    }

    /** 同步文件上传；同 MD5 已成功上传直接复用 video_id。 */
    @Transactional
    public String uploadFile(long advertiserId, Path file, List<String> labels) {
        String md5 = md5(file);
        UploadTask existing = tasks.findByAdvertiserIdAndFileMd5(advertiserId, md5).orElse(null);
        if (existing != null && existing.videoId != null) {
            return existing.videoId;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("advertiser_id", String.valueOf(advertiserId));
        fields.put("upload_type", "UPLOAD_BY_FILE");
        fields.put("video_signature", md5);
        if (labels != null && !labels.isEmpty()) {
            fields.put("labels", toJsonArray(labels));
        }
        MapiResult result = mapi.uploadVideo(advertiserId, fields, file, file.getFileName().toString());
        String videoId = result.data().path("video_id").asText();
        UploadTask row = existing != null ? existing : newRow(advertiserId, md5, file.getFileName().toString());
        row.videoId = videoId;
        row.status = "SUCCESS";
        row.updatedAt = LocalDateTime.now();
        tasks.save(row);
        return videoId;
    }

    /** 异步上传：仅接受连山云 tos/点播链接（契约硬限制），返回任务待轮询。 */
    @Transactional
    public UploadTask uploadByUrl(long advertiserId, String tosUrl, String filename, List<String> labels) {
        String key = md5OfString(tosUrl);
        UploadTask existing = tasks.findByAdvertiserIdAndFileMd5(advertiserId, key).orElse(null);
        if (existing != null && existing.videoId != null) {
            return existing;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("account_id", advertiserId);
        params.put("account_type", "ADVERTISER");
        params.put("filename", filename);
        params.put("video_url", tosUrl);
        if (labels != null && !labels.isEmpty()) {
            params.put("labels", labels);
        }
        MapiResult result = mapi.call(MapiEndpoint.UPLOAD_TASK_CREATE, advertiserId, params);
        UploadTask row = existing != null ? existing : newRow(advertiserId, key, filename);
        row.taskId = result.data().path("task_id").asLong();
        row.status = "PROCESS";
        row.updatedAt = LocalDateTime.now();
        return tasks.save(row);
    }

    /** 轮询所有处理中任务（单批 ≤100 task_ids，契约）。 */
    @Transactional
    public int pollTasks() {
        List<UploadTask> processing = tasks.findByStatus("PROCESS");
        int settled = 0;
        Map<Long, List<UploadTask>> byAdvertiser = new LinkedHashMap<>();
        for (UploadTask task : processing) {
            byAdvertiser.computeIfAbsent(task.advertiserId, k -> new ArrayList<>()).add(task);
        }
        for (Map.Entry<Long, List<UploadTask>> entry : byAdvertiser.entrySet()) {
            List<List<UploadTask>> chunks = ProjectService.chunk(entry.getValue(), TASK_BATCH);
            for (List<UploadTask> chunk : chunks) {
                List<Long> taskIds = chunk.stream().map(t -> t.taskId).toList();
                JsonNode data = mapi.call(MapiEndpoint.UPLOAD_TASK_GET, entry.getKey(),
                        Map.of("account_id", entry.getKey(), "account_type", "ADVERTISER",
                                "task_ids", taskIds),
                        JsonNode.class);
                Map<Long, JsonNode> byTask = new LinkedHashMap<>();
                for (JsonNode item : data.path("list")) {
                    byTask.put(item.path("task_id").asLong(), item);
                }
                for (UploadTask row : chunk) {
                    JsonNode item = byTask.get(row.taskId);
                    if (item == null) {
                        continue;
                    }
                    String status = item.path("status").asText();
                    if ("SUCCESS".equals(status)) {
                        row.videoId = item.path("video_info").path("video_id").asText();
                        row.status = "SUCCESS";
                        settled++;
                    } else if ("FAILED".equals(status)) {
                        row.status = "FAILED";
                        alerts.alert("ERROR", "异步上传失败",
                                "task_id=" + row.taskId + " error=" + item.path("error_msg").asText());
                        settled++;
                    }
                    row.updatedAt = LocalDateTime.now();
                    tasks.save(row);
                }
            }
        }
        return settled;
    }

    /** 标签回读校验：以 file/video/get 返回 labels 为准（评论区 R7 的处置）。 */
    public List<String> readLabels(long advertiserId, String videoId) {
        JsonNode data = mapi.call(MapiEndpoint.VIDEO_GET, advertiserId,
                Map.of("advertiser_id", advertiserId,
                        "filtering", Map.of("video_ids", List.of(videoId)),
                        "page", 1, "page_size", 10),
                JsonNode.class);
        List<String> labels = new ArrayList<>();
        for (JsonNode item : data.path("list")) {
            if (videoId.equals(item.path("id").asText())) {
                item.path("labels").forEach(n -> labels.add(n.asText()));
            }
        }
        return labels;
    }

    public boolean hasLabel(long advertiserId, String videoId, String label) {
        return readLabels(advertiserId, videoId).contains(label);
    }

    private UploadTask newRow(long advertiserId, String md5, String filename) {
        UploadTask row = new UploadTask();
        row.advertiserId = advertiserId;
        row.fileMd5 = md5;
        row.filename = filename;
        row.status = "PROCESS";
        row.createdAt = LocalDateTime.now();
        row.updatedAt = LocalDateTime.now();
        return row;
    }

    static String md5(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (var in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("MD5 计算失败: " + e.getMessage(), e);
        }
    }

    static String md5OfString(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toJsonArray(List<String> labels) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(labels.get(i).replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }
}
