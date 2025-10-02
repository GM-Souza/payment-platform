package com.grupo5.payment_platform.Exceptions;

public class PixQrCodeNotFoundException extends RuntimeException {
    public PixQrCodeNotFoundException(String message) {
        super(message);
    }
}
