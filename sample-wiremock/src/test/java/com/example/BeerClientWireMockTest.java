package com.example;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.wiremock.WireMockSpring;

import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BeerClientWireMockTest {

    WireMockServer wireMock;
    BeerClient client;

    @BeforeEach
    void setup() {
        wireMock = new WireMockServer(WireMockSpring.options().dynamicPort());
        wireMock.start();
        client = new BeerClient("http://localhost:" + wireMock.port());
    }

    @AfterEach
    void teardown() {
        wireMock.stop();
    }

    @Test
    void shouldGrantBeerToAdult() {
        wireMock.stubFor(post(urlEqualTo("/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        assertThat(client.checkAge(22)).isEqualTo("OK");
    }

    @Test
    void shouldDenyBeerToMinor() {
        wireMock.stubFor(post(urlEqualTo("/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"NOT_OK\"}")));

        assertThat(client.checkAge(15)).isEqualTo("NOT_OK");
    }
}
