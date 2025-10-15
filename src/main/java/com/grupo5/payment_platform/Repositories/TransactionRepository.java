package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionModel, UUID> {

    // Consulta válida usando campo existente 'date'
    List<TransactionModel> findByStatusAndDateBefore(TransactionStatus transactionStatus, LocalDateTime limite);

    List<TransactionModel> findByUser_Email(String email);

    List<TransactionModel> findTop5ByUser_IdOrderByDateDesc(UUID userId);
}
