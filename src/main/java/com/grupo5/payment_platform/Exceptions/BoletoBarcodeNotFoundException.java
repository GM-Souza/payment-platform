package com.grupo5.payment_platform.Exceptions;

public class BoletoBarcodeNotFoundException extends RuntimeException {
    public BoletoBarcodeNotFoundException(String message) {
        super(message);
    }
}
