package com.grupo5.payment_platform.DTOs;

import com.grupo5.payment_platform.Models.IndividualModel;

public record IndividualResponseDTO (String fullname, String cpf, String email){

    //metodo para receber um IndividualModel e transformar em IndividualResponseDTO
    public static IndividualResponseDTO fromIndividual(IndividualModel individual){
        return new IndividualResponseDTO(individual.getFullname(), individual.getCpf(), individual.getEmail());
    }
}
