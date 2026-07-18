package com.example;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerPort;
import sh.stubborn.contract.stubrunner.StubsMode;

import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:beer-api-producer-webflux:+:stubs",
        stubsMode = StubsMode.LOCAL
)
class BeerClientTest {

    @StubRunnerPort("beer-api-producer-webflux")
    int producerPort;

    @Test
    void shouldGrantBeerToAdult() {
        BeerClient client = new BeerClient("http://localhost:" + producerPort);
        assertThat(client.checkAge(22).block()).isEqualTo("OK");
    }

    @Test
    void shouldRejectUnderage() {
        BeerClient client = new BeerClient("http://localhost:" + producerPort);
        assertThat(client.checkAge(15).block()).isEqualTo("NOT_OK");
    }
}
