package com.grupo5.payment_platform.Models.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_individual")
@PrimaryKeyJoinColumn(name = "user_id")
public class IndividualModel extends UserModel {

    @Column(name = "fullname",nullable = false)
    private String fullname;

    @CPF
    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

}
