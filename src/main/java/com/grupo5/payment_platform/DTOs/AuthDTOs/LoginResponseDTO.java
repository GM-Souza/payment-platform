package com.grupo5.payment_platform.DTOs.AuthDTOs;


import java.util.UUID;

public record LoginResponseDTO(String token, UUID userId) {
}
