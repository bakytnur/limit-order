package com.example.order.exception;

public class InsufficientBalanceException extends Exception{

    public InsufficientBalanceException(String error) {
        super(error);
    }
}
