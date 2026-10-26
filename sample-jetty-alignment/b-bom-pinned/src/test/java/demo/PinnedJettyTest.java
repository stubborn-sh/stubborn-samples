package demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * b) Stubborn 0.2.0's BOM, which pinned every org.eclipse.jetty artifact to 12.0.x.
 *
 * <p>The pin does what it was meant to: ee10 and the core agree, and the stub server runs.
 * The cost is that an imported BOM outranks the platform BOM, so it moves the
 * application's Jetty too — and it only pinned the artifacts WireMock needed, leaving
 * Boot's own ee11 layer at 12.1.x against a 12.0.x core. The split just moved.
 */
class PinnedJettyTest {

    @Test
    @DisplayName("the BOM has pulled the Jetty core down to 12.0.x, under Boot's 12.1.x")
    void coreJettyIsPinnedDown() {
        assertThat(JettyVersions.core()).startsWith("12.0.");
    }

    @Test
    @DisplayName("ee10 agrees with it, which is why the stub server works here")
    void ee10AgreesWithTheCore() {
        assertThat(JettyVersions.ee10Servlet()).startsWith("12.0.");
        assertThat(JettyVersions.ee10Servlet()).isEqualTo(JettyVersions.core());
    }

    @Test
    @DisplayName("the stub server starts and serves")
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

    @Test
    @DisplayName("but the application itself no longer starts: Boot's ee11 layer is still 12.1.x")
    void applicationNoLongerStarts() {
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DemoApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .properties("server.port=0")
                    .run()) {
                // unreachable on a split classpath
            }
        })
                .hasRootCauseInstanceOf(NoSuchMethodError.class)
                .rootCause()
                .hasMessageContaining("Environment.ensure");
    }
}
