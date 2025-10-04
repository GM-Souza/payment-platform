package com.grupo5.payment_platform.Obsolete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDTO(UUID senderId, BigDecimal amount, UUID receiverId,LocalDateTime date) {
}
