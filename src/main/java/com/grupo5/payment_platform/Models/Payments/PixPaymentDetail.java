package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "tb_pix_payment_details")
@PrimaryKeyJoinColumn(name = "payment_detail_id")
public class PixPaymentDetail extends PaymentDetail {

    @Column(name = "mercado_pago_payment_id")
    private Long mercadoPagoPaymentId; // ID do pagamento gerado pelo MercadoPago

    @Column(name = "amount")
    private BigDecimal amount;
    
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "qr_code_copy_paste", columnDefinition = "TEXT")
    private String qrCodeCopyPaste;
}
