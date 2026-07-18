package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.example:beer-api-producer-stubs-per-consumer:+:stubs",
    stubsMode = StubsMode.LOCAL,
    consumerName = "beer-api-consumer-b"
)
class BeerConsumerBTest {

    @Autowired
    StubFinder stubFinder;

    @Test
    void shouldGetBeerInfo() {
        int port = stubFinder.findStubUrl("com.example", "beer-api-producer-stubs-per-consumer").getPort();
        BeerClient client = new BeerClient("http://localhost:" + port);
        String response = client.getInfo();
        assertThat(response).contains("Beer API");
    }
}
