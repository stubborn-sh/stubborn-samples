package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
class BeerRouter {

    @Bean
    RouterFunction<ServerResponse> routes(BeerHandler handler) {
        return RouterFunctions.route(POST("/check"), handler::check);
    }
}
