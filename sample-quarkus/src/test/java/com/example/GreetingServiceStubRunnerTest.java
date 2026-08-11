package com.example;

import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.quarkus.StubRunnerResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots a {@code @QuarkusTest} with the Stubborn Contract stub runner wired in through Quarkus'
 * own {@link QuarkusTestResource} lifecycle. The producer's stubs (a classpath WireMock mapping
 * under {@code src/test/resources/mappings}) are started on a free port and their URL is published
 * as MicroProfile Config, so {@link GreetingService} — unchanged from production — talks to the
 * stub instead of a real producer.
 */
@QuarkusTest
@QuarkusTestResource(value = StubRunnerResource.class,
        initArgs = { @ResourceArg(name = "ids", value = "com.example:greeting-producer:+:stubs"),
                @ResourceArg(name = "stubsMode", value = "CLASSPATH") })
class GreetingServiceStubRunnerTest {

    @Inject
    GreetingService greetingService;

    // Injected by StubRunnerResource#inject — lets a test resolve stub URLs programmatically.
    StubFinder stubFinder;

    @Test
    void consumesTheStubbedGreeting() {
        assertThat(this.greetingService.fetchGreeting()).isEqualTo("{\"message\":\"Hello from the stubbed producer\"}");
    }

    @Test
    void exposesTheRunningStubToStubFinder() {
        assertThat(this.stubFinder.findStubUrl("com.example", "greeting-producer")).isNotNull();
    }

}
