package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_payment_details")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionModel transaction;

}
