package com.grupo5.payment_platform.Models.Payments;

import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Models.Users.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_boleto_transactions")
@PrimaryKeyJoinColumn(name = "id")
public class BoletoModel extends TransactionModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserModel receiver; // beneficiário

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserModel sender; // sacado

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "final_transaction_date")
    private LocalDateTime finalDate; // data pagamento / cancelamento

    @OneToOne(mappedBy = "boletoTransaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BoletoPaymentDetail boletoPaymentDetail;

    public void attachDetail(BoletoPaymentDetail detail) {
        this.boletoPaymentDetail = detail;
        detail.setBoletoTransaction(this);
    }

    public boolean isPending() {
        return getStatus() == TransactionStatus.PENDING;
    }
}

