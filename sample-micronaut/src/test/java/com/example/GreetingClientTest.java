package com.example;

import jakarta.inject.Inject;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import com.example.stubrunner.StubRunnerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code @MicronautTest} that extends {@link StubRunnerTest}. Because
 * {@code StubRunnerTest} implements Micronaut's {@code TestPropertyProvider}, the producer's stubs
 * are started and their URL published as configuration <em>before</em> the application context is
 * created — so the injected {@link GreetingClient} (whose base URL is a property placeholder)
 * already points at the running stub.
 *
 * <p>The stubs live on the test classpath under
 * {@code src/test/resources/META-INF/com.example/greeting-producer/1.0.0/mappings} (CLASSPATH mode).
 */
@MicronautTest
class GreetingClientTest extends StubRunnerTest {

    @Inject
    GreetingClient greetingClient;

    @Override
    protected String[] stubIds() {
        return new String[] { "com.example:greeting-producer" };
    }

    @Test
    void consumesTheStubbedGreeting() {
        assertThat(this.greetingClient.greeting()).isEqualTo("{\"message\":\"Hello from the stubbed producer\"}");
    }

}
