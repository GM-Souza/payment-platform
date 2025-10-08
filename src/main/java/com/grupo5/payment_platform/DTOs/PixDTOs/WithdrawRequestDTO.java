package com.grupo5.payment_platform.DTOs.PixDTOs;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequestDTO (UUID userId, BigDecimal amount) {
}

