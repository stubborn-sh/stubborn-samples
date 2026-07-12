package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    @Autowired
    @Qualifier("outputChannel")
    private MessageChannel outputChannel;

    public void returnBook(BookReturned bookReturned) {
        this.outputChannel.send(
            MessageBuilder.withPayload(bookReturned)
                .setHeader("BOOK-NAME", bookReturned.bookName)
                .build()
        );
    }
}
