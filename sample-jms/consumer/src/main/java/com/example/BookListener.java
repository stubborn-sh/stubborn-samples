package com.example;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class BookListener {

    private final AtomicReference<String> receivedBookName = new AtomicReference<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @JmsListener(destination = "books")
    public void receiveBook(String payload) throws Exception {
        BookReturned book = objectMapper.readValue(payload, BookReturned.class);
        receivedBookName.set(book.bookName());
    }

    public boolean hasReceived(String bookName) {
        return bookName.equals(receivedBookName.get());
    }
}
