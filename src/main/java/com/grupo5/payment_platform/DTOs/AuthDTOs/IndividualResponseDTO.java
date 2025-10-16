package com.grupo5.payment_platform.DTOs.AuthDTOs;

import com.grupo5.payment_platform.Models.Users.IndividualModel;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;

public record IndividualResponseDTO (
        String fullname,
        String cpf,
        String email,
        BigDecimal balance
) {

    // Método para criar DTO a partir do modelo
    public static IndividualResponseDTO fromIndividual(IndividualModel individual){
        return new IndividualResponseDTO(
                individual.getFullname(),
                individual.getCpf(),
                individual.getEmail(),
                individual.getBalance()
        );
    }
}
