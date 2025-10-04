package com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix;

import java.math.BigDecimal;
import java.util.UUID;

public record PixReceiverRequestDTO(UUID receiverId, BigDecimal amount) {
}
