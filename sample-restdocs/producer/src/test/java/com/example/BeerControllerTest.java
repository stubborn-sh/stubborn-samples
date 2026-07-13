package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import sh.stubborn.contract.wiremock.restdocs.WireMockRestDocs;
import sh.stubborn.contract.wiremock.restdocs.WireMockSnippet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class BeerControllerTest {

    @RegisterExtension
    static RestDocumentationExtension restDocumentation = new RestDocumentationExtension("target/snippets");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void shouldGrantBeerToAdult() throws Exception {
        mockMvc.perform(post("/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\": 22}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andDo(WireMockRestDocs.verify()
                        .jsonPath("$[?(@.age >= 18)]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(document("shouldGrantBeerToAdult", new WireMockSnippet()));
    }

    @Test
    void shouldDenyBeerToMinor() throws Exception {
        mockMvc.perform(post("/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_OK"))
                .andDo(WireMockRestDocs.verify()
                        .jsonPath("$[?(@.age < 18)]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(document("shouldDenyBeerToMinor", new WireMockSnippet()));
    }
}
