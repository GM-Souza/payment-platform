package com.grupo5.payment_platform.Models.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_pix_payment_details")
@Getter
@Setter
public class PixPaymentDetail extends PaymentDetail {


    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "qr_code_copy_paste", columnDefinition = "TEXT")
    private String qrCodeCopyPaste;
}
