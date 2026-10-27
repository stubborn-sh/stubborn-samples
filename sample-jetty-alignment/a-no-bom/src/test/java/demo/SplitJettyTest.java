package demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * a) No Stubborn BOM. Boot manages the Jetty core; nothing manages ee10.
 *
 * <p>This is stubborn-sh/stubborn#142 reproduced. Note what these tests do NOT show: a
 * resolution failure, a conflict warning, anything at build time. Maven is happy, the
 * application is happy, and the stub server is the only thing that finds out.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SplitJettyTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("the application runs on the Jetty core Boot manages — 12.1.x")
    void applicationRunsOnBootsJetty() {
        assertThat(((WebServerApplicationContext) this.context).getWebServer().getClass().getName())
                .contains("Jetty");
        assertThat(JettyVersions.core()).startsWith("12.1.");
    }

    @Test
    @DisplayName("but the ee10 servlet layer underneath WireMock is 12.0.x — a minor line apart")
    void ee10IsADifferentMinorLine() {
        // Loaded without initialising it: running the static initialiser is the very thing
        // that fails below, and we want to read the version first.
        assertThat(JettyVersions.ee10Servlet()).startsWith("12.0.");
    }

    @Test
    @DisplayName("so starting the stub server dies in ServletContextHandler's static initialiser")
    void stubServerCannotStart() {
        assertThatThrownBy(() -> {
            WireMockServer server = new WireMockServer(options().dynamicPort());
            server.start();
        })
                // NoSuchMethodError: Environment.ensure(String) — ee10 12.0 calls the old
                // signature, jetty-util 12.1 only has ensure(String, Class).
                .isInstanceOf(Error.class)
                .hasMessageContaining("Environment.ensure");
    }
}
