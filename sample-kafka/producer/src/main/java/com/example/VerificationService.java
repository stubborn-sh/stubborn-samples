package com.example;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    @SuppressWarnings("rawtypes")
    private final KafkaTemplate kafkaTemplate;

    @SuppressWarnings("rawtypes")
    public VerificationService(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @SuppressWarnings("unchecked")
    public void sendVerification(VerificationEvent event) {
        Message<VerificationEvent> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, "verifications")
                .build();
        kafkaTemplate.send(message);
    }
}
