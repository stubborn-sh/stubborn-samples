package demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classpath matrix — Quarkus.
 *
 * <p>Quarkus serves on Vert.x rather than Jetty, so it has no Jetty of its own to clash
 * with WireMock's. That is exactly why it belongs in the matrix: it pins down that the
 * shaded stub server is not merely a Boot workaround but works wherever we claim support,
 * and it would catch a future Quarkus extension pulling Jetty in.
 */
@QuarkusTest
class QuarkusStubServerTest {

    @Test
    @DisplayName("the embedded stub server starts and serves inside a Quarkus test")
    void stubServerStartsAndServes() throws Exception {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(get(urlEqualTo("/ping")).willReturn(aResponse().withStatus(200).withBody("pong")));

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/ping")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("pong");
        }
        finally {
            server.stop();
        }
    }
}
