package com.grupo5.payment_platform.DTOs;

import com.grupo5.payment_platform.Models.LegalEntityModel;

public record LegalEntityResponseDTO(String legalName, String cnpj, String email) {

    //metodo para receber um LegalEntityModel e transformar para LegalEntityResponseDTO
    public static LegalEntityResponseDTO fromLegalEntity (LegalEntityModel legalEntity) {
        return new LegalEntityResponseDTO(legalEntity.getLegalName(), legalEntity.getCnpj(), legalEntity.getEmail());
    }
}
