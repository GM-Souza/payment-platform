package com.grupo5.payment_platform.Models.Users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="tb_users")
@Inheritance(strategy = InheritanceType.JOINED)
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email",nullable = false,unique = true)
    private String email;

    @Column(name = "balance")
    private BigDecimal balance = BigDecimal.ZERO;


}
