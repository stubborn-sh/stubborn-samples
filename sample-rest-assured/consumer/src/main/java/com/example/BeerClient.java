package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BeerClient {

    private final RestClient restClient;

    BeerClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://localhost:8080").build();
    }

    public String checkAge(int age) {
        return restClient.post()
                .uri("/check")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new PersonToCheck(age))
                .retrieve()
                .body(BeerResponse.class)
                .status();
    }
}
