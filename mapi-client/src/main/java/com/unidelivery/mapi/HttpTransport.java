package com.unidelivery.mapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * java.net.http 传输层：JSON GET/POST 与 multipart 上传。不做业务重试（重试在 MapiClient）。
 */
public final class HttpTransport {

    /** 传输层原始应答。 */
    public record RawResponse(int status, String body) {
    }

    private final HttpClient http;
    private final Duration requestTimeout;

    public HttpTransport(Duration connectTimeout, Duration requestTimeout) {
        this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.requestTimeout = requestTimeout;
    }

    public RawResponse get(String url, String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Access-Token", accessToken)
                .GET()
                .build();
        return send(request);
    }

    /** 自定义鉴权 header（SPI 补拉用 APP-Access-Token）。 */
    public RawResponse getWithHeader(String url, String headerName, String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header(headerName, token)
                .GET()
                .build();
        return send(request);
    }

    public RawResponse postJson(String url, String accessToken, String json)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Access-Token", accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    public RawResponse postMultipart(String url, String accessToken, Map<String, String> fields,
                                     String fileField, Path file, String filename)
            throws IOException, InterruptedException {
        String boundary = "----UniDelivery" + UUID.randomUUID().toString().replace("-", "");
        List<byte[]> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            parts.add(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n"
                    + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        parts.add(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(Files.readAllBytes(file));
        parts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        int total = parts.stream().mapToInt(p -> p.length).sum();
        byte[] body = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, body, offset, part.length);
            offset += part.length;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("Access-Token", accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return send(request);
    }

    private RawResponse send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new RawResponse(response.statusCode(), response.body());
    }
}
