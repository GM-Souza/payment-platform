package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_pix_payment_details")
@Getter
@Setter
public class PixPaymentDetail extends PaymentDetail {

    private Long mercadoPagoPaymentId; // ID do pagamento gerado pelo MercadoPago
    private BigDecimal amount;
    
    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "qr_code_copy_paste", columnDefinition = "TEXT")
    private String qrCodeCopyPaste;
}
