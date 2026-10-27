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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stubborn <= 0.2.0: the BOM pinned every org.eclipse.jetty artifact to the 12.0.x line.
 *
 * <p>Skipped entirely once the pinned Stubborn version is one whose BOM aligns instead —
 * see {@link BomAlignedWithBootTest}, which then takes over. Exactly one of the two runs.
 */
class BomPinsJettyDownTest {

    @org.junit.jupiter.api.BeforeEach
    void onlyForABomThatPinsJettyDown() {
        assumeTrue(!BomAlignment.alignedWithBoot(),
                "the pinned Stubborn BOM aligns ee10 with Boot, so BomAlignedWithBootTest covers it");
    }

    @Test
    @DisplayName("the BOM has pulled Jetty core down to 12.0.x, under the 12.1.x Boot manages")
    void coreJettyIsPinnedDown() {
        assertThat(JettyVersions.core()).startsWith("12.0.");
    }

    @Test
    @DisplayName("ee10 agrees with it, which is the one thing the pin got right")
    void ee10AgreesWithTheCore() {
        assertThat(JettyVersions.ee10Servlet()).isEqualTo(JettyVersions.core());
    }

    @Test
    @DisplayName("so the stub server starts and serves")
    void stubServerStartsAndServes() throws Exception {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(get(urlEqualTo("/ping")).willReturn(aResponse().withStatus(200).withBody("pong")));

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/ping")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.body()).isEqualTo("pong");
        }
        finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("but the application no longer starts: Boot's own ee11 layer is still 12.1.x")
    void applicationNoLongerStarts() {
        // The pin only covered the artifacts WireMock needs. Boot's web server runs on the
        // ee11 servlet layer, which the BOM never mentioned, so it stayed at 12.1.x and now
        // sits on top of a 12.0.x core. The split did not close; it moved.
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
