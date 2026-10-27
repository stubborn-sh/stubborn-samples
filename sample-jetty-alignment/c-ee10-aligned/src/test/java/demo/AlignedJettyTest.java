package demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.verifier.dsl.wiremock.DefaultResponseTransformer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * c) ee10 managed at Boot's own Jetty version — the fix.
 *
 * <p>Both halves work at once, which is the thing neither a) nor b) manages. Nothing here
 * overrides a version Boot has chosen: ee10 is an artifact Boot does not manage at all, so
 * this fills a gap instead of winning an argument.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlignedJettyTest {

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("core and ee10 are the same Jetty, and it is the one Boot manages")
    void everythingIsOnOneJettyLine() {
        assertThat(JettyVersions.core()).startsWith("12.1.");
        assertThat(JettyVersions.ee10Servlet()).isEqualTo(JettyVersions.core());
    }

    @Test
    @DisplayName("the application starts on Boot's Jetty and serves")
    void applicationStartsAndServes() throws Exception {
        assertThat(((WebServerApplicationContext) this.context).getWebServer().getClass().getName())
                .contains("Jetty");

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/hello")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("hello from the application");
    }

    @Test
    @DisplayName("the stub server starts and serves in the same JVM")
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
    @DisplayName("and Stubborn's response-template helpers register and render")
    void stubbornTemplateHelpersWork() throws Exception {
        // The unshaded WireMock keeps Handlebars where it is, so Stubborn's helpers can be
        // registered against it. Compare with module d), where shading breaks this.
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
