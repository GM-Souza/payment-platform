package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_boleto_payment_details")
@Getter
@Setter
public class BoletoPaymentDetail {

    @Id
    @Column(name = "id")
    private UUID id; // mesma PK da transação boleto

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private BoletoModel boletoTransaction;

    @Column(name = "boleto_code", unique = true)
    private String boletoCode;

    @Column(name = "document_code", unique = true)
    private String documentCode;

    @Column(name = "our_number", unique = true)
    private String ourNumber;
}
