package com.example;

import org.springframework.web.client.RestTemplate;

public class BeerClient {

    private final RestTemplate restTemplate;
    private final String url;

    public BeerClient(String url) {
        this.url = url;
        this.restTemplate = new RestTemplate();
    }

    public String getInfo() {
        return restTemplate.getForObject(url + "/info", String.class);
    }
}
