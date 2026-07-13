package com.example;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;

import sh.stubborn.contract.verifier.http.HttpVerifier;
import sh.stubborn.contract.verifier.http.OkHttpHttpVerifier;

@SpringBootTest(
    classes = BeerRestBase.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"spring.grpc.server.port=0"}
)
public abstract class BeerRestBase {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        private volatile int grpcPort;

        @EventListener
        void onGrpcStarted(GrpcServerStartedEvent event) {
            this.grpcPort = event.getPort();
        }

        @Bean
        BeerServiceImpl beerService() {
            return new BeerServiceImpl();
        }

        @Bean
        HttpVerifier httpOkVerifier() {
            return request -> new OkHttpHttpVerifier("localhost:" + grpcPort).exchange(request);
        }
    }
}
