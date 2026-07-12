package com.example;

import org.springframework.web.client.RestClient;

public class BeerClient {

    private final RestClient restClient;

    public BeerClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String checkIfAdult(int age) {
        return restClient.post()
                .uri("/check")
                .header("Content-Type", "application/json")
                .body("{\"age\":" + age + "}")
                .retrieve()
                .body(String.class);
    }
}
