package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.math.BigDecimal;

public record PixPaymentWithCreditCardResponse(String qrCodeCopyPaste, String status, BigDecimal amountPaid, String paymentType) {
}
