package com.grupo5.payment_platform.Models.payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pix_payment_details")
@Getter
@Setter
public class PixPaymentDetail extends PaymentDetail {

    @Column(name = "mercado_pago_payment_id", unique = true)
    private Long mercadoPagoPaymentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "qr_code_copy_paste", columnDefinition = "TEXT")
    private String qrCodeCopyPaste;
}
