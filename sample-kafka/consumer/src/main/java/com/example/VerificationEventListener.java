package com.example;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VerificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEventListener.class);

    private final List<String> receivedBookNames = new CopyOnWriteArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "verifications")
    public void onVerification(String payload) {
        try {
            Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {});
            Object bookName = map.get("bookName");
            if (bookName != null) {
                receivedBookNames.add(bookName.toString());
            }
        }
        catch (Exception e) {
            log.warn("Could not parse payload as JSON, treating as plain string: {}", payload);
            receivedBookNames.add(payload);
        }
    }

    public boolean hasReceived(String bookName) {
        return receivedBookNames.contains(bookName);
    }

    public void reset() {
        receivedBookNames.clear();
    }
}
