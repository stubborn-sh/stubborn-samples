package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;
import sh.stubborn.contract.stubrunner.StubFinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:book-api-producer-messaging:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BookConsumerTest {

    @Autowired
    StubFinder stubFinder;

    @Autowired
    BookReturnedListener listener;

    @BeforeEach
    void reset() {
        listener.reset();
    }

    @Test
    void shouldReceiveBookReturnedEvent() throws Exception {
        stubFinder.trigger("book_returned");

        await().atMost(Duration.ofSeconds(5))
               .untilAsserted(() -> assertThat(listener.hasReceived("foo")).isTrue());
    }
}
