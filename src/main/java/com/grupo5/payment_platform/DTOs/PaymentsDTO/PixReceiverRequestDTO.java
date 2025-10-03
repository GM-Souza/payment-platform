package com.grupo5.payment_platform.DTOs.PaymentsDTO;

import java.math.BigDecimal;
import java.util.UUID;

public record PixReceiverRequestDTO(UUID receiverId, BigDecimal amount) {
}
