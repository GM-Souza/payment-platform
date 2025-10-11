package com.grupo5.payment_platform.Obsolete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRequestDTO(String senderEmail, BigDecimal amount, String receiverEmail, LocalDateTime date) {

    public TransactionRequestDTO(String senderEmail, BigDecimal amount, String receiverEmail) {
        this(senderEmail, amount, receiverEmail, LocalDateTime.now());
    }
}
