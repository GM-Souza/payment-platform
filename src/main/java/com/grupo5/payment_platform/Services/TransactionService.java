package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.TransactionResponseDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.UserModel;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.grupo5.payment_platform.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;


    public TransactionService (TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransactionModel createTransaction(TransactionResponseDTO dto) throws Exception {
        UserModel sender = this.userRepository.findById(dto.senderId())
                .orElseThrow(() -> new Exception("Usuario pagador não encontrado."));

        UserModel receiver = this.userRepository.findById(dto.receiverId())
                .orElseThrow(() -> new Exception("Usuario recebedor não encontrado."));

        sender.setBalance(sender.getBalance().subtract(dto.amount()));
        receiver.setBalance(receiver.getBalance().add(dto.amount()));

        TransactionModel newTransaction = new TransactionModel();
        newTransaction.setSender(sender);
        newTransaction.setReceiver(sender);
        newTransaction.setAmount(dto.amount());
        newTransaction.setDate(LocalDateTime.now());
        transactionRepository.save(newTransaction);

        return newTransaction;
    }
}
