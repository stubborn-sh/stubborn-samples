package com.example;

import org.springframework.web.client.RestTemplate;

public class BeerClient {

    private final RestTemplate restTemplate;
    private final String url;

    public BeerClient(String url) {
        this.url = url;
        this.restTemplate = new RestTemplate();
    }

    public String checkAge(int age) {
        return restTemplate.postForObject(url + "/check",
            new AgeRequest(age), String.class);
    }

    record AgeRequest(int age) {}
}
