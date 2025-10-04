package com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto;

import java.math.BigDecimal;
import java.util.UUID;

public record BoletoSenderResponseDTO(UUID transactionId, String status, BigDecimal amount) {
}
