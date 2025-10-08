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
@Table(name = "tb_pixTransactions")
@PrimaryKeyJoinColumn(name = "id")
public class PixModel extends TransactionModel{

    @Column(name = "final_transaction_date")
    private LocalDateTime finalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private UserModel receiver;

    @OneToOne
    @JoinColumn(name = "id", nullable = false)
    private PixPaymentDetail pixPaymentDetail;

}
