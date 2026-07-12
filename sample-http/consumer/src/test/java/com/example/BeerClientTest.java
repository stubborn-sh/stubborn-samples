package com.example;

import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubRunnerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureStubRunner(
        ids = "com.example:beer-api-producer:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BeerClientTest {

    @LocalServerPort
    int port;

    @Test
    void shouldGrantBeerToAdult() {
        BeerClient client = new BeerClient("http://localhost:" + port);
        // stub runner serves the producer's stubs; verify consumer handles the response
        assertThat(client.checkIfAdult(22)).contains("OK");
    }

    @Test
    void shouldRejectUnderage() {
        BeerClient client = new BeerClient("http://localhost:" + port);
        assertThat(client.checkIfAdult(15)).contains("NOT_OK");
    }
}
