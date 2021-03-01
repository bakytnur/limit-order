package com.example.order.exception;

public class RecordAlreadyExistsException extends Exception{

    public RecordAlreadyExistsException(String error) {
        super(error);
    }
}
