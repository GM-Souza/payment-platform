package com.grupo5.payment_platform.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDTO(
        UUID id,
        BigDecimal amount,
        LocalDateTime date,
        String status,
        String paymentType,
        String userEmail
) {}
