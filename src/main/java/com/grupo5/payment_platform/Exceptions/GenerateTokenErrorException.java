package com.grupo5.payment_platform.Exceptions;

public class GenerateTokenErrorException extends RuntimeException {
    public GenerateTokenErrorException(String message) {
        super(message);
    }
}
