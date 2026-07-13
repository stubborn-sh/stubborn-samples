package com.example;

import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class BeerServiceImpl extends BeerServiceGrpc.BeerServiceImplBase {

    @Override
    public void check(PersonToCheck request, StreamObserver<Response> responseObserver) {
        Response.BeerCheckStatus status = request.getAge() >= 18
            ? Response.BeerCheckStatus.OK
            : Response.BeerCheckStatus.NOT_OK;

        responseObserver.onNext(Response.newBuilder().setStatus(status).build());
        responseObserver.onCompleted();
    }
}
