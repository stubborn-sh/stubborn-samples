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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stubborn is built and tested against Spring Boot 4.1. This asks whether a Boot 4.0
 * application can use it too, and the answer has to be demonstrated on each Boot version
 * rather than reasoned about, because the thing that broke in gh-142 resolved perfectly
 * well and only failed when a server started.
 *
 * <p>Nothing here configures Jetty. That is the point: with Stubborn 0.2.1 the BOM
 * manages the ee10 servlet layer Boot leaves unmanaged, and leaves everything else to
 * Boot, so a consumer writes no import and no exclusion whatever Boot version they are on.
 */
class BootCompatibilityTest {

    @Test
    @DisplayName("Boot manages Jetty on the 12.1 line")
    void bootManagesJettyOnThe121Line() {
        assertThat(JettyVersions.BOOT_JETTY)
            .as("boot.jetty.version was not passed to the tests; see this module's surefire config")
            .isNotEmpty();

        assertThat(JettyVersions.core())
            .as("Boot %s manages Jetty %s", JettyVersions.BOOT, JettyVersions.BOOT_JETTY)
            .isEqualTo(JettyVersions.BOOT_JETTY)
            .startsWith("12.1.");
    }

    @Test
    @DisplayName("Stubborn supplies ee10 on that same minor line, patch gap and all")
    void stubbornSuppliesEe10OnTheSameMinorLine() {
        String core = JettyVersions.core();
        String ee10 = JettyVersions.ee10Servlet();

        // The interesting case is Boot 4.0.0, where the core is 12.1.4 and ee10 is 12.1.14.
        // gh-142 was a MINOR-line split and was fatal; a patch gap inside one line is not,
        // and the tests below exercise it rather than taking that on trust.
        assertThat(JettyVersions.minorLine(ee10))
            .as("ee10 %s and core %s must share a minor line (Boot %s)", ee10, core, JettyVersions.BOOT)
            .isEqualTo(JettyVersions.minorLine(core));
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
    @DisplayName("the stub server starts and serves in the same JVM")
    void stubServerStartsAndServes() throws Exception {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        try {
            server.stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse().withStatus(200).withBody("pong")));

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
    @DisplayName("and Stubborn's response-template helpers render")
    void stubbornTemplateHelpersRender() throws Exception {
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
