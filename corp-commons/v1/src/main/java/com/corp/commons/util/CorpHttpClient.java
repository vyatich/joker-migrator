package com.corp.commons.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client for calls between corporate services.
 */
public class CorpHttpClient {
    private final String baseUrl;
    private final long timeoutMillis;
    private final HttpClient client;

    /**
     * @param baseUrl       base address of the target service
     * @param timeoutMillis request timeout in milliseconds
     */
    public CorpHttpClient(String baseUrl, long timeoutMillis) {
        this.baseUrl = baseUrl;
        this.timeoutMillis = timeoutMillis;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMillis)).build();
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofMillis(timeoutMillis))
            .header("X-Corp-Service", CorpContext.current().getServiceName())
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " from " + request.uri());
        }
        return response.body();
    }
}
