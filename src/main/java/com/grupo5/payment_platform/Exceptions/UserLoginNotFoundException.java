package com.grupo5.payment_platform.Exceptions;

public class UserLoginNotFoundException extends RuntimeException {
    public UserLoginNotFoundException(String message) {
        super(message);
    }
}
