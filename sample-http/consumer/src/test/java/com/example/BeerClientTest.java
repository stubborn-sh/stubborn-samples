package com.example;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerPort;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:beer-api-producer:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BeerClientTest {

    @StubRunnerPort("beer-api-producer")
    int producerPort;

    @Test
    void shouldGrantBeerToAdult() {
        BeerClient client = new BeerClient("http://localhost:" + producerPort);
        assertThat(client.checkIfAdult(22)).contains("OK");
    }

    @Test
    void shouldRejectUnderage() {
        BeerClient client = new BeerClient("http://localhost:" + producerPort);
        assertThat(client.checkIfAdult(15)).contains("NOT_OK");
    }
}
