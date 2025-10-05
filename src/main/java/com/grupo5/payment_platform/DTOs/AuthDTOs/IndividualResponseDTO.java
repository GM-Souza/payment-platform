package com.grupo5.payment_platform.DTOs.AuthDTOs;

import com.grupo5.payment_platform.Models.Users.IndividualModel;

import java.math.BigDecimal;

public record IndividualResponseDTO (String fullname, String cpf, String email, BigDecimal balance) {

    //metodo para receber um IndividualModel e transformar em IndividualResponseDTO
    public static IndividualResponseDTO fromIndividual(IndividualModel individual){
        return new IndividualResponseDTO(individual.getFullname(), individual.getCpf(), individual.getEmail(), individual.getBalance());
    }
}
