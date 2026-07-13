package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
class BeerHandler {

    Mono<ServerResponse> check(ServerRequest request) {
        return request.bodyToMono(PersonToCheck.class)
                .flatMap(person -> {
                    String status = person.age() >= 20 ? "OK" : "NOT_OK";
                    return ServerResponse.ok().bodyValue(new BeerResponse(status));
                });
    }
}
