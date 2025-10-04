package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverResponseDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderResponseDTO;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO;
import com.grupo5.payment_platform.Services.TransactionsServices.TransactionService;
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
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto) {
        TransactionModel newTransaction = transactionService.createTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    //Endpoint para gerar cobrança pix
    @PostMapping("/pix")
    public ResponseEntity<PixReceiverResponseDTO> createPixTransaction(@RequestBody PixReceiverRequestDTO dto) throws Exception {
        PixPaymentDetail pixDetail = transactionService.gerarCobrancaPix(dto);

        //Montando o response DTO
        PixReceiverResponseDTO response = new PixReceiverResponseDTO(
                pixDetail.getTransaction().getId(),
                pixDetail.getTransaction().getStatus().toString(), // PENDING
                pixDetail.getQrCodeBase64(), pixDetail.getQrCodeCopyPaste()
        );

        return ResponseEntity.ok(response);
    }


    //Endpoint para pagar via pix
    @PostMapping("/pagar-copy-paste")
    public ResponseEntity<PixSenderResponseDTO> pagarViaPixCopyPaste(@RequestBody PixSenderRequestDTO request) throws Exception {
        TransactionModel transacao = transactionService.pagarViaPixCopyPaste(request);

        PixSenderResponseDTO response = new PixSenderResponseDTO(transacao.getId(), transacao.getStatus().toString(), transacao.getAmount());

        return ResponseEntity.ok(response);
    }

}