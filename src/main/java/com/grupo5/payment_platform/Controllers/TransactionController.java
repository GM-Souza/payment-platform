package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.DTOs.TransactionResponseDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto) throws Exception {
        TransactionModel newTransaction = this.transactionService.createTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }
}
