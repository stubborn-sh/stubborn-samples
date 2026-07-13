package com.example;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BookListener {

    private final List<String> receivedBooks = new CopyOnWriteArrayList<>();

    @Bean
    public Consumer<BookReturned> books() {
        return book -> receivedBooks.add(book.bookName());
    }

    public boolean hasReceived(String bookName) {
        return receivedBooks.contains(bookName);
    }

    public void reset() {
        receivedBooks.clear();
    }
}
