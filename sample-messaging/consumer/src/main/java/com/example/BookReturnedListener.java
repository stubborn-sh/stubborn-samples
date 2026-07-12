package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@MessageEndpoint
public class BookReturnedListener {

    private static final Logger log = LoggerFactory.getLogger(BookReturnedListener.class);

    private final List<String> receivedBookNames = new CopyOnWriteArrayList<>();

    @ServiceActivator(inputChannel = "output")
    @SuppressWarnings("unchecked")
    public void onBookReturned(Message<?> message) {
        Object payload = message.getPayload();
        String bookName = null;
        if (payload instanceof Map<?, ?> map) {
            bookName = (String) map.get("bookName");
        } else if (payload instanceof BookReturnedEvent event) {
            bookName = event.bookName;
        } else if (payload instanceof String json) {
            bookName = json.contains("foo") ? "foo" : null;
        }
        if (bookName != null) {
            log.info("Received book-returned event for: {}", bookName);
            receivedBookNames.add(bookName);
        }
    }

    public boolean hasReceived(String bookName) {
        return receivedBookNames.contains(bookName);
    }

    public void reset() {
        receivedBookNames.clear();
    }
}
