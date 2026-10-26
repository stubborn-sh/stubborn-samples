package demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * d) The shaded wiremock-standalone, whose Jetty is relocated under
 * wiremock.org.eclipse.jetty.
 *
 * <p>This is a real fix for the Jetty half: nothing is shared, so nothing can collide, and
 * it would keep working even if Boot and WireMock never agreed on a Jetty again. The reason
 * it is not the answer is in the last test — the same shading relocates Handlebars, and
 * Stubborn's response-template helpers are compiled against the unrelocated one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShadedStandaloneTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("the application keeps Boot's Jetty, because WireMock no longer shares it")
    void applicationKeepsBootsJetty() {
        assertThat(((WebServerApplicationContext) this.context).getWebServer().getClass().getName())
                .contains("Jetty");
        assertThat(JettyVersions.core()).startsWith("12.1.");
    }

    @Test
    @DisplayName("there is no unshaded ee10 on the classpath at all — WireMock brought its own, relocated")
    void wireMockCarriesItsOwnRelocatedJetty() {
        assertThatThrownBy(JettyVersions::ee10Servlet)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("no unshaded jetty-ee10-servlet");

        // Not an ee10 at all: the standalone build bundles the older org.eclipse.jetty.servlet
        // layer, relocated out of the way. Which Jetty it is stops mattering once it is
        // nobody else's Jetty.
        assertThat(classIsPresent("wiremock.org.eclipse.jetty.servlet.ServletContextHandler")).isTrue();
        assertThat(classIsPresent("org.eclipse.jetty.servlet.ServletContextHandler")).isFalse();
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

            assertThat(response.body()).isEqualTo("pong");
        }
        finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("but Stubborn's response-template helpers cannot be registered against it")
    void stubbornTemplateHelpersCannotBeRegistered() {
        // DefaultResponseTransformer hands WireMock a com.github.jknack.handlebars.Helper.
        // The shaded build only knows wiremock.com.github.jknack.handlebars.Helper, so the
        // two never meet. Module c) keeps the unshaded WireMock and does not have this
        // problem, which is why the fix went there.
        assertThatThrownBy(() -> {
            Class<?> transformer = Class.forName("sh.stubborn.contract.verifier.dsl.wiremock.DefaultResponseTransformer");
            Object instance = transformer.getDeclaredConstructor().newInstance();
            WireMockServer server = new WireMockServer(options().dynamicPort()
                    .extensions((com.github.tomakehurst.wiremock.extension.Extension) instance));
            server.start();
            server.stop();
        })
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("cannot be cast to")
                .hasMessageContaining("wiremock.com.github.jknack.handlebars.Helper");
    }

    private static boolean classIsPresent(String name) {
        try {
            Class.forName(name, false, ShadedStandaloneTest.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
