package demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.dsl.wiremock.DefaultResponseTransformer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stubborn 0.2.1 and later: the BOM manages only the ee10 layer, at the Jetty version Boot
 * already uses, and leaves the core alone.
 *
 * <p>This is the same pom as {@link BomPinsJettyDownTest} verifies — no ee10 import, no
 * shading, no exclusions, just Boot's BOM and Stubborn's. These tests are skipped while the
 * pinned Stubborn version is 0.2.0 and start running the moment the version bump lands,
 * which is the whole point: the fix has to show up here without the sample changing.
 */
class BomAlignedWithBootTest {

    @org.junit.jupiter.api.BeforeEach
    void onlyForABomThatAlignsWithBoot() {
        assumeTrue(BomAlignment.alignedWithBoot(),
                "the pinned Stubborn BOM still pins Jetty down to 12.0.x (0.2.0 and earlier); "
                        + "BomPinsJettyDownTest covers that");
    }

    @Test
    @DisplayName("Jetty core is Boot's, untouched by Stubborn's BOM")
    void coreJettyIsBoots() {
        assertThat(JettyVersions.core()).startsWith("12.1.");
    }

    @Test
    @DisplayName("and ee10 has been brought up to meet it, with no help from the application")
    void ee10IsAlignedWithTheCore() {
        assertThat(JettyVersions.ee10Servlet()).isEqualTo(JettyVersions.core());
    }

    @Test
    @DisplayName("the application starts on Boot's Jetty and serves")
    void applicationStartsAndServes() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0")
                .run()) {

            assertThat(((WebServerApplicationContext) context).getWebServer().getClass().getName())
                    .contains("Jetty");

            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/hello")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.body()).isEqualTo("hello from the application");
        }
    }

    @Test
    @DisplayName("the stub server starts, serves, and renders Stubborn's response-template helpers")
    void stubServerStartsAndRendersTemplates() throws Exception {
        WireMockServer server = new WireMockServer(options().dynamicPort()
                .extensions(new DefaultResponseTransformer()));
        server.start();
        try {
            server.stubFor(post(urlEqualTo("/echo")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"name\":\"{{jsonpath request.body '$.name'}}\"}")
                    .withTransformers("response-template")));

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/echo"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"stubborn\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.body()).isEqualTo("{\"name\":\"stubborn\"}");
        }
        finally {
            server.stop();
        }
    }
}
