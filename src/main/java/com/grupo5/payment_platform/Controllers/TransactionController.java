package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.BoletosDTOs.BoletoRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverResponseDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderResponseDTO;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO;
import com.grupo5.payment_platform.Services.BoletoServices.BoletoService;
import com.grupo5.payment_platform.Services.TransactionsServices.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;



@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final BoletoService boletoService;

    public TransactionController(TransactionService transactionService, BoletoService boletoService) {
        this.transactionService = transactionService;
        this.boletoService = boletoService;
    }

    //Endpoint para metodo de transferencia interna p2p
    @PostMapping
    public ResponseEntity<TransactionModel> createTransaction(@RequestBody TransactionRequestDTO dto) {
        TransactionModel newTransaction = transactionService.createTransaction(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    @PostMapping("/saque")
    public ResponseEntity<TransactionModel> createDeposit(@RequestBody PixReceiverRequestDTO dto) {
        TransactionModel newTransaction = transactionService.withdrawFunds(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    @PostMapping("/deposito")
    public ResponseEntity<TransactionModel> createWithdraw(@RequestBody PixReceiverRequestDTO dto) {
        TransactionModel newTransaction = transactionService.depositFunds(dto);
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
    public ResponseEntity<PixSenderResponseDTO> pagarViaPixCopyPaste(@RequestBody PixSenderRequestDTO request){
        TransactionModel transacao = transactionService.pagarViaPixCopyPaste(request);

        PixSenderResponseDTO response = new PixSenderResponseDTO(transacao.getId(), transacao.getStatus().toString(), transacao.getAmount());

        return ResponseEntity.ok(response);
    }
    //Endpoint para listar todas as transações de um usuário
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<String>> listarTransacoes(@PathVariable UUID userId) {
        List<String> resumo = transactionService.listAllTransactions(userId);
        return ResponseEntity.ok(resumo);
    }

    @PostMapping("/generateBoleto")
    public ResponseEntity<byte[]> generateBoleto(@RequestBody BoletoRequestDTO dto, HttpServletResponse response) throws Exception {
        // Gera o PDF via service (que salva a transação e boleto detail conforme a arquitetura)
        byte[] pdfBytes = boletoService.generateBoletoPdf(dto);

        // Headers para PDF (attachment para download; mude para "inline" se quiser visualizar no browser)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "boleto.pdf");

        // Retorna o PDF como byte array
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}