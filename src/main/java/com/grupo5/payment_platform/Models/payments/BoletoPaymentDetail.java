package com.grupo5.payment_platform.Models.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_boleto_payment_details")
@Getter
@Setter
public class BoletoPaymentDetail extends PaymentDetail{

    @Column(name = "barcode", nullable = false, unique = true)
    private String barcode;

    @Column(name = "mercado_pago_payment_id", unique = true)
    private Long mercadoPagoPaymentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
}
