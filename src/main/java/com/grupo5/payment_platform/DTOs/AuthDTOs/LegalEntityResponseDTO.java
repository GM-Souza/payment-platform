package com.grupo5.payment_platform.DTOs.AuthDTOs;

import com.grupo5.payment_platform.Models.Users.LegalEntityModel;

import java.math.BigDecimal;

public record LegalEntityResponseDTO(String legalName, String cnpj, String email, BigDecimal balance) {

    //metodo para receber um LegalEntityModel e transformar para LegalEntityResponseDTO
    public static LegalEntityResponseDTO fromLegalEntity (LegalEntityModel legalEntity) {
        return new LegalEntityResponseDTO(legalEntity.getLegalName(), legalEntity.getCnpj(), legalEntity.getEmail(),legalEntity.getBalance());
    }
}
