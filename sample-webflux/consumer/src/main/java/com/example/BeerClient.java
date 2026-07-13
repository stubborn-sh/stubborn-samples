package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BeerClient {

    private final String baseUrl;

    BeerClient() {
        this("http://localhost:8080");
    }

    public BeerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Mono<String> checkAge(int age) {
        return WebClient.create(baseUrl)
                .post()
                .uri("/check")
                .bodyValue(new PersonToCheck(age))
                .retrieve()
                .bodyToMono(BeerResponse.class)
                .map(BeerResponse::status);
    }
}
