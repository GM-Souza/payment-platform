package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.util.UUID;

public record PixSenderRequestDTO(String senderEmail, String qrCodeCopyPaste) {
}
