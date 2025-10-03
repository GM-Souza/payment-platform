package com.grupo5.payment_platform.DTOs.PaymentsDTO;

import java.util.UUID;

public record BoletoSenderRequestDTO(UUID senderId, String barcode) {
}
