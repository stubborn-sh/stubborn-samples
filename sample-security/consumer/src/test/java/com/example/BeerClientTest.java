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
        ids = "com.example:beer-api-producer-security:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BeerClientTest {

    @StubRunnerPort("beer-api-producer-security")
    int producerPort;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = producerPort;
        RestAssured.authentication = RestAssured.basic("user", "password");
    }

    @Test
    void shouldGrantBeerToAdult() {
        given()
                .contentType("application/json")
                .body("{\"age\": 22}")
        .when()
                .post("/check")
        .then()
                .statusCode(200)
                .body("status", equalTo("OK"));
    }
}
