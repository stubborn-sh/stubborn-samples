package demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;

import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The compatibility question Jetty hides.
 *
 * <p>Stubborn's stub-runner starter is compiled against Spring Boot 4.1. A consumer on
 * Boot 4.0 resolves it with their own Boot version, so its autoconfiguration runs against
 * 4.0's Spring Boot classes. Classpath alignment says nothing about that; only loading the
 * context does.
 */
@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(stubsMode = StubsMode.CLASSPATH, ids = {})
class StubRunnerAutoConfigurationTest {

    @Test
    @DisplayName("the stub-runner autoconfiguration loads on this Boot version")
    void autoConfigurationLoads() {
        assertThat(JettyVersions.BOOT).isNotEmpty();
    }
}
