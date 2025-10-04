package com.grupo5.payment_platform.DTOs.CardDTOs;

import java.math.BigDecimal;

public record CreditCardPaymentRequest(String senderId, BigDecimal amount, String cardToken) {
}
