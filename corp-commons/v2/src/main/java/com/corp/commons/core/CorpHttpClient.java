package com.corp.commons.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * HTTP client for calls between corporate services.
 */
public class CorpHttpClient {
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient client;

    /**
     * @param baseUrl base address of the target service
     * @param timeout request timeout
     */
    public CorpHttpClient(String baseUrl, Duration timeout) {
        this.baseUrl = baseUrl;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public String get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(timeout)
            .header("X-Corp-Service", CorpContext.current().getServiceName())
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " from " + request.uri());
        }
        return response.body();
    }
}
