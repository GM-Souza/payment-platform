package com.grupo5.payment_platform.DTOs;

import java.util.UUID;

public record PixResponseDTO(UUID transactionId, String status, String qrCodeBase64, String qrCodeCopyPaste) {
}
