package com.grupo5.payment_platform.DTOs.BoletosDTOs;

import java.math.BigDecimal;

public record BoletoResponseDTO(String emailSender, String emailReceiver , BigDecimal amount) {
}
