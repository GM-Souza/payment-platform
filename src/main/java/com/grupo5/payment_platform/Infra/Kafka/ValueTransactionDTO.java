package com.grupo5.payment_platform.Infra.Kafka;

import com.grupo5.payment_platform.Enums.EmailSubject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ValueTransactionDTO(String email, BigDecimal value, LocalDateTime date, EmailSubject type) {
}
