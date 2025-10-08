package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawResponseDTO(UUID userId, BigDecimal amount) {
}

