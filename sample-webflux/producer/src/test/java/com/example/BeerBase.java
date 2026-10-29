package com.example;

import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Base class for the generated WEBTESTCLIENT tests.
 *
 * <p>WebTestClient binds straight to the Spring context, so unlike the EXPLICIT and
 * MOCKMVC modes there is no servlet layer and no HTTP server: no {@code RANDOM_PORT}, no
 * {@code @LocalServerPort}, nothing listening. That is what makes this the mode to reach
 * for in a WebFlux application — the generated test exercises the real
 * {@code RouterFunction} and handler on the reactive stack.
 */
@SpringBootTest
public abstract class BeerBase {

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setup() {
        RestAssuredWebTestClient.applicationContextSetup(context);
    }
}
