package com.example;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:kafka-api-producer:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@EmbeddedKafka(topics = "verifications", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class VerificationConsumerTest {

    @Autowired
    StubFinder stubFinder;

    @Autowired
    VerificationEventListener listener;

    @BeforeEach
    void reset() {
        listener.reset();
    }

    @Test
    void shouldReceiveVerificationEvent() {
        stubFinder.trigger("verification_received");

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                    assertThat(listener.hasReceived("foo")).isTrue()
                );
    }
}
