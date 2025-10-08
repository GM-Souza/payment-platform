package com.grupo5.payment_platform.Models.Payments;

import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Models.Users.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_transactions")
@Inheritance(strategy = InheritanceType.JOINED)
public class TransactionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserModel user;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "solicitation_date")
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "payment_type")
    private String paymentType; // Ex: PIX, CREDIT_CARD, DEBIT_CARD, etc.

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PaymentDetail paymentDetail;}
