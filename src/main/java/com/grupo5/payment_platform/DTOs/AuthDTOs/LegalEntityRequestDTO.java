package com.grupo5.payment_platform.DTOs.AuthDTOs;

import java.math.BigDecimal;

public record LegalEntityRequestDTO(String legalName, String cnpj, String email, String password, BigDecimal balance) {
}
