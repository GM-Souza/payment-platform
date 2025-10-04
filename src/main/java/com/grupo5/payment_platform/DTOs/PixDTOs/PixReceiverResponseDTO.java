package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.util.UUID;

public record PixReceiverResponseDTO(UUID transactionId, String status, String qrCodeBase64, String qrCodeCopyPaste) {

}
