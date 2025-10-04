package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.util.UUID;

public record PixPaymentWithCreditCardRequest(String qrCodeCopyPaste,UUID senderId,String cardToken) {
}
