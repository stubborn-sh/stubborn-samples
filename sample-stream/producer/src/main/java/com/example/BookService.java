package com.example;

import java.util.function.Supplier;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final StreamBridge streamBridge;

    BookService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void returnBook(BookReturned book) {
        streamBridge.send("books-out-0", book);
    }
}
