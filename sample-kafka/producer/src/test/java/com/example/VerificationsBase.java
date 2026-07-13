package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

@SpringBootTest
@AutoConfigureMessageVerifier
@EmbeddedKafka(topics = "verifications", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public abstract class VerificationsBase {

    @Autowired
    VerificationService verificationService;

    @BeforeEach
    void setup() {
    }

    public void verificationEventTriggered() {
        verificationService.sendVerification(new VerificationEvent("foo"));
    }
}
