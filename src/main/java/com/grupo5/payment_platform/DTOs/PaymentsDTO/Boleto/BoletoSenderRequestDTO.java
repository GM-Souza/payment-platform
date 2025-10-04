package com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto;

import java.util.UUID;

public record BoletoSenderRequestDTO(UUID senderId, String barcode) {
}
