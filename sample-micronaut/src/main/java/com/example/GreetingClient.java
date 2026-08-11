package com.example;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

/**
 * A Micronaut declarative HTTP client for the "greeting producer".
 *
 * <p>The base URL is resolved from configuration. In tests, {@code StubRunnerTest} publishes the
 * running stub's URL under exactly this key <em>before</em> the application context starts, so this
 * unchanged client talks to the in-process stub instead of a real producer.
 */
@Client("${stubborn.contract.stubrunner.runningstubs.greeting-producer.url}")
public interface GreetingClient {

    @Get("/greeting")
    String greeting();

}
