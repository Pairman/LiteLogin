package org.eu.pnxlr.git.litelogin.core.http;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal HTTP helper backed by the JDK HttpClient.
 */
public final class HttpClientHelper {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private HttpClientHelper() {
    }

    public static String getString(String url, long timeoutMillis, int retry, long retryDelayMillis) throws IOException {
        return sendForString(buildRequest(url, timeoutMillis, Map.of()).GET().build(), retry, retryDelayMillis);
    }

    public static byte[] getBytes(String url, long timeoutMillis, int retry, long retryDelayMillis) throws IOException {
        return sendForBytes(buildRequest(url, timeoutMillis, Map.of()).GET().build(), retry, retryDelayMillis);
    }

    public static String postJson(String url, String jsonBody, long timeoutMillis, int retry, long retryDelayMillis) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(CONTENT_TYPE_HEADER, "application/json");
        HttpRequest request = buildRequest(url, timeoutMillis, headers)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return sendForString(request, retry, retryDelayMillis);
    }

    private static HttpRequest.Builder buildRequest(String url, long timeoutMillis, Map<String, String> extraHeaders) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header(USER_AGENT_HEADER, LiteLoginConstants.HTTP_USER_AGENT);
        extraHeaders.forEach(builder::header);
        return builder;
    }

    private static String sendForString(HttpRequest request, int retry, long retryDelayMillis) throws IOException {
        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString(), retry, retryDelayMillis);
        return response.body();
    }

    private static byte[] sendForBytes(HttpRequest request, int retry, long retryDelayMillis) throws IOException {
        HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray(), retry, retryDelayMillis);
        return response.body();
    }

    private static <T> HttpResponse<T> send(
            HttpRequest request,
            HttpResponse.BodyHandler<T> bodyHandler,
            int retry,
            long retryDelayMillis
    ) throws IOException {
        int attempt = 0;
        while (true) {
            logRequest(request, attempt);
            long startNanos = System.nanoTime();
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(request.timeout().orElse(Duration.ofSeconds(30)))
                        .build();
                HttpResponse<T> response = client.send(request, bodyHandler);
                long tookMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                LoggerProvider.getLogger().debug(String.format(
                        "<-- %d %s (%dms)",
                        response.statusCode(),
                        request.uri(),
                        tookMillis
                ));
                if (response.statusCode() >= 400) {
                    throw new IOException(String.format("HTTP %d from %s", response.statusCode(), request.uri()));
                }
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while sending HTTP request.", e);
            } catch (IOException e) {
                LoggerProvider.getLogger().debug(String.format("HTTP request failed on attempt %d.", attempt + 1), e);
                if (attempt >= retry) {
                    throw e;
                }
                sleep(retryDelayMillis);
                attempt++;
            }
        }
    }

    private static void logRequest(HttpRequest request, int attempt) {
        String retrySuffix = attempt == 0 ? "" : String.format(" (retry %d)", attempt);
        LoggerProvider.getLogger().debug(String.format("--> %s %s%s",
                request.method(),
                request.uri(),
                retrySuffix
        ));
    }

    private static void sleep(long retryDelayMillis) throws IOException {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to retry HTTP request.", e);
        }
    }
}
