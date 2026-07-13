package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BeerClient {

    private final String baseUrl;

    BeerClient() {
        this("http://localhost:8080");
    }

    public BeerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String checkAge(int age) {
        return RestClient.create(baseUrl)
                .post().uri("/check")
                .body(new PersonToCheck(age))
                .retrieve()
                .body(BeerResponse.class)
                .status();
    }
}
