package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

@SpringBootTest
@AutoConfigureMessageVerifier
public abstract class BooksBase {

    @Autowired
    BookService bookService;

    public void bookReturnedTriggered() {
        bookService.returnBook(new BookReturned("foo"));
    }
}
