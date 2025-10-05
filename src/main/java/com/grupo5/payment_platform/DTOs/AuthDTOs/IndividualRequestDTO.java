package com.grupo5.payment_platform.DTOs.AuthDTOs;

import java.math.BigDecimal;

public record IndividualRequestDTO (String fullname, String cpf, String email, String password,BigDecimal balance) {
}
