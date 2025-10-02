package com.grupo5.payment_platform.DTOs;

import java.util.UUID;

public record PixSenderRequestDTO(UUID senderId, String qrCodeCopyPaste) {
}
