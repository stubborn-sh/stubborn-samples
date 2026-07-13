package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import sh.stubborn.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

@SpringBootTest
@AutoConfigureMessageVerifier
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
public abstract class BooksBase {

    @Autowired
    BookService bookService;

    public void bookReturned() {
        bookService.returnBook(new BookReturned("foo"));
    }
}
