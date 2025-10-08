package com.grupo5.payment_platform.Enums;

public enum EmailSubject {
    WELCOME("Welcome to AgiPay!"),
    PAYMENT_SUCESS("Payment Successful"),
    PAYMENT_RECEIVED("Payment Received");

    private final String subject;

    EmailSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }
}
