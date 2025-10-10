package com.grupo5.payment_platform.Controllers;

import ch.qos.logback.core.model.Model;
import com.grupo5.payment_platform.DTOs.Cards.CreditCardRequestDTO;
import com.grupo5.payment_platform.DTOs.Cards.CreditCardResponseDTO;
import com.grupo5.payment_platform.DTOs.Cards.PagCreditCardRequestDTO;
import com.grupo5.payment_platform.DTOs.Cards.PagCreditCardResponseDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.*;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.BoletoRequestDTO;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoRequestDTO;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoResponseDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverResponseDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderResponseDTO;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO;
import com.grupo5.payment_platform.Services.BoletoServices.BoletoService;
import com.grupo5.payment_platform.Services.TransactionsServices.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/deposito")
    public ResponseEntity<TransactionModel> createDeposit(@RequestBody DepositRequestDTO dto) {
        TransactionModel newTransaction = transactionService.depositFunds(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    @PostMapping("/saque")
    public ResponseEntity<TransactionModel> createWithdraw(@RequestBody WithdrawRequestDTO dto) {
        TransactionModel newTransaction = transactionService.withdrawFunds(dto);
        return new ResponseEntity<>(newTransaction, HttpStatus.OK);
    }

    //Endpoint para gerar cobrança pix
    @PostMapping("/pix")
    public ResponseEntity<PixReceiverResponseDTO> createPixTransaction(@RequestBody PixReceiverRequestDTO dto) throws Exception {
        PixPaymentDetail pixDetail = transactionService.gerarCobrancaPix(dto);

        //Montando o response DTO
        PixReceiverResponseDTO response = new PixReceiverResponseDTO(
                pixDetail.getPixTransaction().getId(),
                pixDetail.getPixTransaction().getStatus().toString(), // PENDING
                pixDetail.getQrCodeBase64(), pixDetail.getQrCodeCopyPaste()
        );

        return ResponseEntity.ok(response);
    }

    //Endpoint para pagar via pix
    @PostMapping("/pagar-copy-paste")
    public ResponseEntity<PixSenderResponseDTO> pagarViaPixCopyPaste(@RequestBody PixSenderRequestDTO request) {
        TransactionModel transacao = transactionService.pagarViaPixCopyPaste(request);

        PixSenderResponseDTO response = new PixSenderResponseDTO(transacao.getId(), transacao.getStatus().toString(), transacao.getAmount());

        return ResponseEntity.ok(response);
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

    @PostMapping("/pagarBoleto")
    public ResponseEntity<PagBoletoResponseDTO> pagarViaPixCopyPaste(@RequestBody PagBoletoRequestDTO request) {
        TransactionModel transacao = transactionService.pagarViaCodigoBoleto(request);

        PagBoletoResponseDTO response = new PagBoletoResponseDTO(request.codeBoleto());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-credit-card")
    public ResponseEntity<CreditCardResponseDTO> createCreditCard(@RequestBody CreditCardRequestDTO request) {
        // Cria o cartão usando o service
        CreditCardModel card = transactionService.createCreditCard(request);

        // Constrói o DTO de resposta
        CreditCardResponseDTO response = new CreditCardResponseDTO(
                card.getCreditNumber(),
                card.getCvv(),
                card.getExpiration().toString(),  // LocalDate -> String
                card.getCreditLimit().toString()        // BigDecimal -> String
        );

        return ResponseEntity.ok(response);
    }



    @GetMapping("/get-card")
    public ResponseEntity<CreditCardResponseDTO> getCreditCard(@RequestBody CreditCardRequestDTO request) {
        // Busca o cartão usando o service
        CreditCardModel card = transactionService.getCreditCardByEmail(request);


        // Constrói o DTO de resposta
        CreditCardResponseDTO response = new CreditCardResponseDTO(
                card.getCreditNumber(),
                card.getCvv(),
                card.getExpiration().toString(),  // LocalDate -> String
                card.getCreditLimit().toString()        // BigDecimal -> String
        );
        return ResponseEntity.ok(response);
    }

        @PostMapping("/pagar-fatura-cartao")
        public ResponseEntity<PagCreditCardResponseDTO> pagarUltimaFatura(@RequestBody PagCreditCardRequestDTO request) {
            CreditInvoiceModel invoice = transactionService.pagarUltimaFaturaComSaldo(request);

            // Constrói o DTO de resposta
            PagCreditCardResponseDTO response = new PagCreditCardResponseDTO(
                    invoice.getId().toString(),
                    invoice.getCreditCardId().getId().toString(),
                    invoice.getClosingDate().toString(),
                    invoice.getTotalAmount().toString(),
                    String.valueOf(invoice.isPaid())
            );

            return ResponseEntity.ok(response);
        }





}