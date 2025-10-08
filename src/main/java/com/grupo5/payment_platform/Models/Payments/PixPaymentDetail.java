package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_pix_payment_details")
@Getter
@Setter
public class PixPaymentDetail {

    @Id
    @Column(name = "id")
    private UUID id; // mesma PK da transação Pix

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private PixModel pixTransaction;

    private Long mercadoPagoPaymentId; // ID do pagamento gerado pelo MercadoPago

    @Lob
    @Column(name = "qr_code_base64", columnDefinition = "LONGTEXT")
    private String qrCodeBase64;

    @Lob
    @Column(name = "qr_code_copy_paste", columnDefinition = "LONGTEXT")
    private String qrCodeCopyPaste;
}
