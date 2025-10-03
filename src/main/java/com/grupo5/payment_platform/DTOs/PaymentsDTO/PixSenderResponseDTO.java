package com.grupo5.payment_platform.DTOs.PaymentsDTO;

import java.math.BigDecimal;
import java.util.UUID;

public record PixSenderResponseDTO(UUID transactionId, String status, BigDecimal amount) {

}
