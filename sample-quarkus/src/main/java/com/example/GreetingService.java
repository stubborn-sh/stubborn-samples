package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A tiny Quarkus consumer bean that calls a remote "greeting producer" over HTTP.
 *
 * <p>The producer's base URL is injected from MicroProfile Config. In production this would be
 * the real producer's address; in tests the {@code StubRunnerResource} adapter publishes the
 * address of the in-process WireMock stub under the same key, so the exact same code runs
 * against the producer's published stubs.
 */
@ApplicationScoped
public class GreetingService {

    private final String producerUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GreetingService(
            @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.greeting-producer.url",
                    defaultValue = "http://localhost:8080") String producerUrl) {
        this.producerUrl = producerUrl;
    }

    /**
     * Calls {@code GET /greeting} on the producer and returns the raw response body.
     * @return the greeting payload served by the producer (or its stub)
     */
    public String fetchGreeting() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(this.producerUrl + "/greeting")).GET().build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status " + response.statusCode() + " from producer");
            }
            return response.body();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to call greeting producer at " + this.producerUrl, ex);
        }
    }

}
