package com.example;

import jakarta.inject.Inject;

import io.helidon.microprofile.testing.junit5.HelidonTest;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.StubsMode;
import sh.stubborn.contract.stubrunner.helidon.StubRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Helidon MP test that boots the producer's stubs with {@code @StubRunner} and the application
 * with {@code @HelidonTest}.
 *
 * <p><strong>Annotation order matters:</strong> {@code @StubRunner} is declared <em>before</em>
 * {@code @HelidonTest} so its {@code beforeAll} runs first and sets the stub-port System property
 * before Helidon snapshots its MicroProfile Config. The injected {@link GreetingService} then reads
 * that port and talks to the in-process stub.
 *
 * <p>The stubs live on the test classpath under
 * {@code src/test/resources/META-INF/com.example/greeting-producer/1.0.0/mappings} (CLASSPATH mode).
 */
@StubRunner(ids = "com.example:greeting-producer:+:stubs", stubsMode = StubsMode.CLASSPATH)
@HelidonTest
class GreetingServiceStubRunnerTest {

    @Inject
    GreetingService greetingService;

    @Test
    void consumesTheStubbedGreeting() {
        assertThat(this.greetingService.fetchGreeting()).isEqualTo("{\"message\":\"Hello from the stubbed producer\"}");
    }

    @Test
    void exposesTheRunningStubToStubFinder(StubFinder stubFinder) {
        assertThat(stubFinder.findStubUrl("com.example", "greeting-producer")).isNotNull();
    }

}
