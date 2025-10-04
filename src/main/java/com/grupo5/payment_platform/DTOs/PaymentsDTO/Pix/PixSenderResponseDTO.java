package com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix;

import java.math.BigDecimal;
import java.util.UUID;

public record PixSenderResponseDTO(UUID transactionId, String status, BigDecimal amount) {

}
