package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A Helidon MicroProfile consumer bean that calls a remote "greeting producer" over HTTP.
 *
 * <p>The producer's port is injected from MicroProfile Config. In tests, the Stubborn Contract
 * Helidon extension publishes the running stub's port under this key (via a System property, which
 * Helidon MP Config reads by default) <em>before</em> the CDI container starts — so this unchanged
 * bean talks to the in-process stub instead of a real producer.
 */
@ApplicationScoped
public class GreetingService {

    private final int producerPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    public GreetingService(
            @ConfigProperty(name = "stubborn.contract.stubrunner.runningstubs.greeting-producer.port",
                    defaultValue = "8080") int producerPort) {
        this.producerPort = producerPort;
    }

    /**
     * Calls {@code GET /greeting} on the producer and returns the raw response body.
     * @return the greeting payload served by the producer (or its stub)
     */
    public String fetchGreeting() {
        try {
            URI uri = URI.create("http://localhost:" + this.producerPort + "/greeting");
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status " + response.statusCode() + " from producer");
            }
            return response.body();
        }
        catch (IllegalStateException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to call greeting producer on port " + this.producerPort, ex);
        }
    }

}
