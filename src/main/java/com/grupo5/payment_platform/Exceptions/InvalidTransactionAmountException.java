package com.grupo5.payment_platform.Exceptions;

public class InvalidTransactionAmountException extends RuntimeException{
    public InvalidTransactionAmountException(String message) {
        super(message);
    }
}
