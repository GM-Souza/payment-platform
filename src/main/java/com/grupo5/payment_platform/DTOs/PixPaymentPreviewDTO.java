package com.grupo5.payment_platform.DTOs;

import java.math.BigDecimal;

public record PixPaymentPreviewDTO(String receiverEmail, BigDecimal amount) {
}
