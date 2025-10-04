package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Entity
@Getter
@Setter
@Table(name = "tb_credit_card_payment_detail")
@PrimaryKeyJoinColumn(name = "payment_detail_id")
public class CreditCardPaymentDetail extends PaymentDetail{

    @Column(name = "mercado_pago_payment_id")
    private Long mercadoPagoPaymentId;

    @Column(name = "card_last_four_digits")
    private String cardLastFourDigits;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "installments")
    private Integer installments;


}
