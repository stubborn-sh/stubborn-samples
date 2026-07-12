package com.example;

import java.io.Serializable;

@SuppressWarnings("serial")
public class BookReturned implements Serializable {

    public String bookName;

    public BookReturned() {}

    public BookReturned(String bookName) {
        this.bookName = bookName;
    }
}
