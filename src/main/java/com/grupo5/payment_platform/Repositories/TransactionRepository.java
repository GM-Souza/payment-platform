package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionModel, UUID> {

}
