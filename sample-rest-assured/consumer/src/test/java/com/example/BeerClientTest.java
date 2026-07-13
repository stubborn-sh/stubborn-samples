package com.example;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerPort;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.example:beer-api-producer-restassured:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BeerClientTest {

    @StubRunnerPort("beer-api-producer-restassured")
    int producerPort;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = producerPort;
    }

    @Test
    void shouldGrantBeerToAdult() {
        given()
            .contentType("application/json")
            .body("{\"age\":22}")
        .when()
            .post("/check")
        .then()
            .statusCode(200)
            .body("status", equalTo("OK"));
    }

    @Test
    void shouldRejectUnderage() {
        given()
            .contentType("application/json")
            .body("{\"age\":15}")
        .when()
            .post("/check")
        .then()
            .statusCode(200)
            .body("status", equalTo("NOT_OK"));
    }
}
