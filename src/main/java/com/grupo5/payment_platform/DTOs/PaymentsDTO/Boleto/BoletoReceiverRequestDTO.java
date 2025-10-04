package com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto;

import java.math.BigDecimal;
import java.util.UUID;

public record BoletoReceiverRequestDTO(UUID receiverId, BigDecimal amount) {
}
