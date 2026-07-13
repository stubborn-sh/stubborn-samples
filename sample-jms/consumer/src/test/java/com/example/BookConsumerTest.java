package com.example;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(ids = "com.example:book-api-producer-jms:+:stubs", stubsMode = StubRunnerProperties.StubsMode.LOCAL)
class BookConsumerTest {

    @Autowired
    StubFinder stubFinder;

    @Autowired
    BookListener listener;

    @Test
    void shouldReceiveBookReturnedEvent() {
        stubFinder.trigger("book_returned");
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(listener.hasReceived("foo")).isTrue());
    }
}
