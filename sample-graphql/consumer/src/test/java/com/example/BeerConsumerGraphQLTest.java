package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.spring.StubRunnerProperties;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.example:beer-api-producer-graphql:+:stubs",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class BeerConsumerGraphQLTest {

    private static final String REQUEST_BODY = """
            {
              "query": "query queryName($personName: String!) {\\n  personToCheck(name: $personName) {\\n    name\\n    age\\n  }\\n}",
              "variables": {"personName": "Old Enough"},
              "operationName": "queryName"
            }
            """;

    @Autowired
    StubFinder stubFinder;

    @Test
    void should_send_a_graphql_request() {
        int port = stubFinder.findStubUrl("com.example", "beer-api-producer-graphql").getPort();
        ResponseEntity<String> response = new RestTemplate()
            .exchange(
                RequestEntity.post(URI.create("http://localhost:" + port + "/graphql"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(REQUEST_BODY),
                String.class
            );
        then(response.getStatusCode().value()).isEqualTo(200);
        then(response.getBody()).contains("Old Enough");
    }
}
