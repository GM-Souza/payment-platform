package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionModel, UUID> {
   List<TransactionModel> findByStatusAndDateBefore(TransactionStatus transactionStatus, LocalDateTime limite);

    List<TransactionModel> findByStatusAndCreateDateBefore(TransactionStatus transactionStatus, LocalDateTime limite);

    //Metodo para listar todas as transações de um usuário, seja como remetente ou destinatário
    List<TransactionModel> findBySenderIdOrReceiverId(UUID sender,UUID receiver);

}
