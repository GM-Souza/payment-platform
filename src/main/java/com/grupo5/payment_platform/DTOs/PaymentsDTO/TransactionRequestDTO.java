package com.grupo5.payment_platform.DTOs.PaymentsDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRequestDTO(UUID senderId, BigDecimal amount, UUID receiverId, LocalDateTime date) {

    public TransactionRequestDTO(UUID senderId, BigDecimal amount, UUID receiverId) {
        this(senderId, amount, receiverId, LocalDateTime.now());
    }
}
