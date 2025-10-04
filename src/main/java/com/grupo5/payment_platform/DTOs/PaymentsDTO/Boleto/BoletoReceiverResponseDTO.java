package com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto;

import java.util.UUID;

public record BoletoReceiverResponseDTO(UUID transactionId, String status, String barcode) {
}
