package com.grupo5.payment_platform.DTOs;

import java.math.BigDecimal;
import java.util.UUID;

public record PixReceiverRequestDTO(UUID receiverId, BigDecimal amount) {
}
