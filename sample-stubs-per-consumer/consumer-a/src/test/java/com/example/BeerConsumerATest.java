package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.example:beer-api-producer-stubs-per-consumer:+:stubs",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    consumerName = "beer-api-consumer-a"
)
class BeerConsumerATest {

    @Autowired
    StubFinder stubFinder;

    @Test
    void shouldGrantBeerToAdult() {
        int port = stubFinder.findStubUrl("com.example", "beer-api-producer-stubs-per-consumer").getPort();
        BeerClient client = new BeerClient("http://localhost:" + port);
        String response = client.checkAge(22);
        assertThat(response).contains("OK");
    }
}
