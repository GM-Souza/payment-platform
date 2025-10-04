package com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix;

import java.util.UUID;

public record PixSenderRequestDTO(UUID senderId, String qrCodeCopyPaste) {
}
