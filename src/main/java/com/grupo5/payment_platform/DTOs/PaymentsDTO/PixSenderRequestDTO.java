package com.grupo5.payment_platform.DTOs.PaymentsDTO;

import java.util.UUID;

public record PixSenderRequestDTO(UUID senderId, String qrCodeCopyPaste) {
}
