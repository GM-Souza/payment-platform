package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.PixResponseDTO;
import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.DTOs.TransactionResponseDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
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

    //Endpoint para metodo de transferencia interna p2p
    @PostMapping
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto) throws Exception {
        TransactionModel newTransaction = this.transactionService.createTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    //Endpoint para metodo de transferencia pix via mp
    @PostMapping("/pix")
    public ResponseEntity<PixResponseDTO> createPixTransaction(@RequestBody TransactionRequestDTO dto) throws Exception {
       TransactionModel pixTransaction = transactionService.createPixTransaction(dto);

        PixPaymentDetail pixPaymentDetail = (PixPaymentDetail)  pixTransaction.getPaymentDetail();
        PixResponseDTO pixResponse = new PixResponseDTO(pixTransaction.getId().toString(),pixTransaction.getStatus().toString(),pixPaymentDetail.getQrCodeBase64(),pixPaymentDetail.getQrCodeCopyPaste());
        return ResponseEntity.ok(pixResponse);
    }
}
