package com.example;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final JmsTemplate jmsTemplate;

    BookService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void returnBook(BookReturned book) {
        jmsTemplate.convertAndSend("books", book, message -> {
            message.setStringProperty("contentType", "application/json");
            return message;
        });
    }
}
