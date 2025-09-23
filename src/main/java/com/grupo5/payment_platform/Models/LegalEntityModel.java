package com.grupo5.payment_platform.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_legal_entity")
@PrimaryKeyJoinColumn(name = "user_id")
public class LegalEntityModel extends UserModel {

    @Column(name = "legal_name",nullable = false)
    private String legalName;

    @CNPJ
    @Column(name = "cnpj", nullable = false, unique = true)
    private String cnpj;

}
