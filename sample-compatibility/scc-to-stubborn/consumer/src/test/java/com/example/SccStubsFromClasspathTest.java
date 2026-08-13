package com.example;

import org.junit.jupiter.api.Test;

import sh.stubborn.contract.stubrunner.StubFinder;
import sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner;
import sh.stubborn.contract.stubrunner.StubsMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Same SCC → Stubborn compatibility as {@link SccStubsFromStubbornRunnerTest}, but in
 * {@code CLASSPATH} mode: the Spring Cloud Contract stubs jar is on the test classpath
 * (a {@code stubs}-classified test dependency), and Stubborn resolves it straight off the
 * classpath — scanning the installed {@code META-INF/<group>/<artifact>/<version>/mappings}
 * layout inside the jar — rather than from the local Maven repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
    ids = "com.example:compat-scc-producer:+:stubs",
    stubsMode = StubsMode.CLASSPATH
)
class SccStubsFromClasspathTest {

    @Autowired
    StubFinder stubFinder;

    @Test
    void stubborn_stub_runner_can_serve_scc_stubs_from_the_classpath() {
        int port = stubFinder.findStubUrl("com.example", "compat-scc-producer").getPort();

        ResponseEntity<String> response = new RestTemplate().exchange(
            RequestEntity.post(URI.create("http://localhost:" + port + "/check"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"age\":22}"),
            String.class
        );

        then(response.getStatusCode().value()).isEqualTo(200);
        then(response.getBody()).isEqualTo("OK");
    }
}
