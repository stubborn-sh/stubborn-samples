package com.example;

import java.util.Map;

import org.springframework.web.client.RestClient;

public class BeerClient {

    private final RestClient restClient;

    public BeerClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @SuppressWarnings("unchecked")
    public String checkIfAdult(int age) {
        Map<String, String> response = restClient.post()
                .uri("/check")
                .header("Content-Type", "application/json")
                .body("{\"age\":" + age + "}")
                .retrieve()
                .body(Map.class);
        return response != null ? response.get("status") : null;
    }
}
