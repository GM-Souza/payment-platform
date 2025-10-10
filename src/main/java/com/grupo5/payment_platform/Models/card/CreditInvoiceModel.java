package com.grupo5.payment_platform.Models.card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_credit_invoice")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditInvoiceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCardModel creditCardId;

    @Column(name = "closing_date",nullable = false)
    private LocalDate closingDate; // data de fechamento da fatura

    @Column(name = "due_date",nullable = false)
    private LocalDate dueDate; // data de vencimento (pagamento)

    @Column(name = "total_amount",nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid",nullable = false)
    private boolean paid = false;


    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ParcelModel> parcels = new ArrayList<>();

    // Método utilitário pra somar (opcional, pra recalcular totalAmount se precisar)
    public void recalculateTotalAmount() {
        this.totalAmount = parcels.stream()
                .map(ParcelModel::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
