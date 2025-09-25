package com.grupo5.payment_platform.Exceptions;

public class LegalEntityNotFoundException extends RuntimeException{

    public LegalEntityNotFoundException(String message) {
        super(message);
    }
}
