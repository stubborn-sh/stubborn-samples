package demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classpath matrix — Spring Boot 4.1 on Jetty.
 *
 * <p>Starting the stub server is the assertion. A split Jetty classpath does not show up
 * as a resolution error; it shows up here, as a NoSuchMethodError the moment WireMock
 * touches a Jetty class that the other Jetty on the classpath has changed.
 */
class BootJettyStubServerTest {

    @Test
    @DisplayName("the embedded stub server starts and serves alongside Spring Boot 4.1 on Jetty")
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
    @DisplayName("the application keeps the Jetty its framework manages, not WireMock's")
    void applicationKeepsItsOwnJetty() {
        String version = org.eclipse.jetty.server.Server.class.getPackage().getImplementationVersion();

        // Boot 4.1 manages Jetty 12.1.x (jetty-bom + jetty-ee11-bom). If this reads 12.0.x
        // something has pinned Jetty back down -- which is what used to break
        // spring-boot-starter-jetty applications.
        assertThat(version).startsWith("12.1.");
    }

    @Test
    @DisplayName("and the stub server shares it: ee10 is on the same Jetty, not a line below")
    void stubServerSharesTheApplicationsJetty() throws Exception {
        Class<?> handler = Class.forName("org.eclipse.jetty.ee10.servlet.ServletContextHandler", false,
                getClass().getClassLoader());

        assertThat(handler.getPackage().getImplementationVersion())
                .isEqualTo(org.eclipse.jetty.server.Server.class.getPackage().getImplementationVersion());
    }

    @Test
    @DisplayName("Stubborn's response-template helpers render, which the shaded WireMock could not do")
    void stubbornTemplateHelpersRender() throws Exception {
        // The reason this module runs the ordinary WireMock rather than wiremock-standalone:
        // shading relocates Handlebars too, and these helpers cannot then be registered.
        WireMockServer server = new WireMockServer(options().dynamicPort()
                .extensions(new sh.stubborn.contract.verifier.dsl.wiremock.DefaultResponseTransformer()));
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
