package com.grupo5.payment_platform.Models.card;

import com.grupo5.payment_platform.Models.Payments.BoletoModel;
import com.grupo5.payment_platform.Models.Payments.PixModel;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
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
@Table(name = "tb_parcels")
public class ParcelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CreditInvoiceModel invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id", nullable = false)
    private TransactionModel originalTransaction; // BoletoModel ou PixModel

    @Column(name = "parcel_number", nullable = false)
    private int parcelNumber; // Ex: 1 de 3

    @Column(name = "total_parcels", nullable = false)
    private int totalParcels; // Ex: 3

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description")
    private String description; // Ex: "Parcela de Boleto #123" ou "Parcela de PIX #456"
}