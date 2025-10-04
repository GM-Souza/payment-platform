package com.grupo5.payment_platform.DTOs.IndivualDTOs;

import java.math.BigDecimal;

public record IndividualRequestDTO (String fullname, String cpf, String email, BigDecimal balance) {
}
