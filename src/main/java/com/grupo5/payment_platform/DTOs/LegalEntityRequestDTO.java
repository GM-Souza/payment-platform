package com.grupo5.payment_platform.DTOs;

import java.math.BigDecimal;

public record LegalEntityRequestDTO(String legalName, String cnpj, String email, BigDecimal balance) {
}
