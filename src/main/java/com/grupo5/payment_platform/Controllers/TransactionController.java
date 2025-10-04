package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.PaymentsDTO.*;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto.BoletoReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto.BoletoReceiverResponseDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto.BoletoSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Boleto.BoletoSenderResponseDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix.PixReceiverResponseDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PaymentsDTO.Pix.PixSenderResponseDTO;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.payments.BoletoPaymentDetail;
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
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto){
        TransactionModel newTransaction = transactionService.createTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    // -----------------------------
    // GERAR COBRANÇA PIX
    // -----------------------------
    @PostMapping("/pix")
    public ResponseEntity<PixReceiverResponseDTO> createPixTransaction(@RequestBody PixReceiverRequestDTO dto) throws Exception {
        PixPaymentDetail pixDetail = transactionService.gerarCobrancaPix(dto);

        PixReceiverResponseDTO response = new PixReceiverResponseDTO(
                pixDetail.getTransaction().getId(),
                pixDetail.getTransaction().getStatus().toString(), // PENDING
                pixDetail.getQrCodeBase64(),
                pixDetail.getQrCodeCopyPaste()
        );

        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // PAGAR COBRANÇA PIX
    // -----------------------------
    @PostMapping("/pagar-copy-paste")
    public ResponseEntity<PixSenderResponseDTO> pagarViaPixCopyPaste(@RequestBody PixSenderRequestDTO request) throws Exception {
        TransactionModel transacao = transactionService.pagarViaPixCopyPaste(request);

        PixSenderResponseDTO response = new PixSenderResponseDTO(
                transacao.getId(),
                transacao.getStatus().toString(),
                transacao.getAmount()
        );

        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // GERAR COBRANÇA BOLETO
    // -----------------------------
    @PostMapping("/boleto")
    public ResponseEntity<BoletoReceiverResponseDTO> createBoletoTransaction(@RequestBody BoletoReceiverRequestDTO dto) throws Exception {
       BoletoPaymentDetail boletoDetail = transactionService.gerarCobrancaBoleto(dto);

        BoletoReceiverResponseDTO response = new BoletoReceiverResponseDTO(
                boletoDetail.getId(),
                boletoDetail.getTransaction().getStatus().toString(),
                boletoDetail.getBarcode());

        return ResponseEntity.ok(response);
    }

    // -----------------------------
    // PAGAR COBRANÇA BOLETO
    // -----------------------------
    @PostMapping("/pagar-boleto")
    public ResponseEntity<BoletoSenderResponseDTO> pagarViaBoleto(@RequestBody BoletoSenderRequestDTO request) throws Exception {
        TransactionModel transacao = transactionService.pagarCobrancaBoleto(request);

        BoletoSenderResponseDTO response = new BoletoSenderResponseDTO(
                transacao.getId(),
                transacao.getStatus().toString(),
                transacao.getAmount()
        );

        return ResponseEntity.ok(response);
    }

}