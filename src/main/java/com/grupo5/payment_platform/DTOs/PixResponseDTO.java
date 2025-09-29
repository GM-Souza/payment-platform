package com.grupo5.payment_platform.DTOs;

public record PixResponseDTO(String transactionId, String status, String qrCodeBase64, String qrCodeCopyPaste) {
}
