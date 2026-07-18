package com.example;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:book-api-producer-stream:+:stubs",
        stubsMode = StubsMode.LOCAL
)
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
class BookConsumerTest {

    @Autowired
    StubFinder stubFinder;

    @Autowired
    BookListener listener;

    @BeforeEach
    void reset() {
        listener.reset();
    }

    @Test
    void shouldReceiveBookReturnedEvent() {
        stubFinder.trigger("book_returned");

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(listener.hasReceived("foo")).isTrue());
    }
}
