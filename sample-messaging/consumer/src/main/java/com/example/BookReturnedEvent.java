package com.example;

import java.io.Serializable;

@SuppressWarnings("serial")
public class BookReturnedEvent implements Serializable {

    public String bookName;

    public BookReturnedEvent() {}

    public BookReturnedEvent(String bookName) {
        this.bookName = bookName;
    }
}
