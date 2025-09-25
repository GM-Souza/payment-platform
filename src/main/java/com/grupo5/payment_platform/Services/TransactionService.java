package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.UserModel;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserService userService;


    public TransactionService (TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional
    public TransactionModel createTransaction(TransactionRequestDTO dto) throws Exception {
        UserModel sender = this.userService.findById(dto.senderId());
        UserModel receiver = this.userService.findById(dto.receiverId());

        sender.setBalance(sender.getBalance().subtract(dto.amount()));
        receiver.setBalance(receiver.getBalance().add(dto.amount()));

        TransactionModel newTransaction = new TransactionModel();
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setAmount(dto.amount());
        newTransaction.setDate(LocalDateTime.now());
        transactionRepository.save(newTransaction);

        return newTransaction;
    }
}
