package com.grupo5.payment_platform.DTOs;

import com.grupo5.payment_platform.Models.IndividualModel;

import java.math.BigDecimal;

public record IndividualResponseDTO (String fullname, String cpf, String email, BigDecimal balance) {

    //metodo para receber um IndividualModel e transformar em IndividualResponseDTO
    public static IndividualResponseDTO fromIndividual(IndividualModel individual){
        return new IndividualResponseDTO(individual.getFullName(), individual.getCpf(), individual.getEmail(), individual.getBalance());
    }
}
