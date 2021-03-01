package com.example.order.exception;

public class AmountTooLowException extends Exception {
    public AmountTooLowException(String error) {
        super(error);
    }
}
