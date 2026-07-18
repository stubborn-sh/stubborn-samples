package com.example;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.example:beer-api-producer-grpc:+:stubs",
    stubsMode = StubsMode.LOCAL
)
class GrpcTests {

    @Autowired
    StubFinder stubFinder;

    ManagedChannel channel;
    BeerServiceGrpc.BeerServiceBlockingStub stub;

    @BeforeEach
    void setup() {
        int port = stubFinder.findStubUrl("com.example", "beer-api-producer-grpc").getPort();
        channel = ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .intercept(fixedStatusSendingClientInterceptor())
            .build();
        stub = BeerServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void teardown() throws InterruptedException {
        channel.shutdownNow();
    }

    @Test
    void should_give_me_a_beer_when_im_old_enough() {
        Response response = stub.check(PersonToCheck.newBuilder().setAge(23).build());
        assertThat(response.getStatus()).isEqualTo(Response.BeerCheckStatus.OK);
    }

    @Test
    void should_reject_a_minor() {
        Response response = stub.check(PersonToCheck.newBuilder().setAge(17).build());
        assertThat(response.getStatus()).isEqualTo(Response.BeerCheckStatus.NOT_OK);
    }

    /**
     * WireMock cannot return grpc-status in HTTP/2 trailers,
     * so we intercept the response and force Status.OK.
     */
    private static ClientInterceptor fixedStatusSendingClientInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                super.onClose(Status.OK, trailers);
                            }
                        }, headers);
                    }
                };
            }
        };
    }
}
