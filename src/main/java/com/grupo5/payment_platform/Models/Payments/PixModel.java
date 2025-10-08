package com.grupo5.payment_platform.Models.Payments;

import com.grupo5.payment_platform.Models.Users.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_pixTransactions")
@PrimaryKeyJoinColumn(name = "id")
public class PixModel extends TransactionModel {

    @Column(name = "final_transaction_date")
    private LocalDateTime finalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserModel receiver;

    @OneToOne(mappedBy = "pixTransaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PixPaymentDetail pixPaymentDetail;

    public void attachDetail(PixPaymentDetail detail) {
        this.pixPaymentDetail = detail;
        detail.setPixTransaction(this);
    }
}
