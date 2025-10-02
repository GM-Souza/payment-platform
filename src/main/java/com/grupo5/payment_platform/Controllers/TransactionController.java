package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.PixResponseDTO;
import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
import com.grupo5.payment_platform.Services.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    //Endpoint para metodo de transferencia interna p2p
    @PostMapping
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto){
        TransactionModel newTransaction = transactionService.createPixTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    //Endpoint para metodo de transferencia pix via mp
    @PostMapping("/pix")
    public ResponseEntity<PixResponseDTO> createPixTransaction(@RequestBody TransactionRequestDTO dto) throws Exception {
       PixPaymentDetail pixTransaction = transactionService.gerarCobrancaPix(dto);

        PixPaymentDetail pixPaymentDetail = (PixPaymentDetail)  pixTransaction.getTransaction().getPaymentDetail();
        PixResponseDTO pixResponse = new PixResponseDTO(pixTransaction.getId(),pixTransaction.getTransaction().getStatus().toString(),pixPaymentDetail.getQrCodeBase64(),pixPaymentDetail.getQrCodeCopyPaste());
        return ResponseEntity.ok(pixResponse);
    }

    @PostMapping("/pix{qrcode}")
    public ResponseEntity<PixResponseDTO> payPixTransaction(@PathVariable String qrcode) throws Exception {
       TransactionModel pixTransaction = transactionService.pagarViaPixCopyPaste(qrcode);
       PixResponseDTO responseDTO = new PixResponseDTO(pixTransaction.getId(), pixTransaction.getStatus().toString(), null, null);
       return ResponseEntity.ok(responseDTO);
    }
}