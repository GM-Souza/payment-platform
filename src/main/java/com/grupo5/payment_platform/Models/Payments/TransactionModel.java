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
    @JoinColumn(name = "sender_id")
    private UserModel sender;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "create_transaction_date")
    private LocalDateTime createDate;

    @Column(name = "final_transaction_date")
    private LocalDateTime finalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserModel receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "mp_payment_id")
    private Long mercadoPagoPaymentId;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PaymentDetail paymentDetail;



}
