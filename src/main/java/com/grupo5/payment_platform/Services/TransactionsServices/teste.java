
/*
package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.*;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Repositories.BoletoRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


Então mesmo que for uma pessoa x pagando um boleto que deveria ser a pessoa y,
e tendo em mente que ao criar o boleto eu ja coloquei na tabela transação a pessoa y,
eu posso dntro dste codigo de pagaf boleto, atualizar a pessoa Y para a pessoa x...


public class teste {

    private final BoletoRepository boletoRepository;
    private final TransactionRepository transactionRepository;

    public teste(BoletoRepository boletoRepository, TransactionRepository transactionRepository) {
        this.boletoRepository = boletoRepository;
        this.transactionRepository = transactionRepository;
    }

    // PAGAR BOLETO
    @Transactional
    public TransactionModel pagarViaCodigoBoleto(PagBoletoRequestDTO dto){

        BoletoPaymentDetail boletoPaymentDetail = boletoRepository.findByBoletoCode(dto.codeBoleto()).orElseThrow(()->
                new CodeBoletoNotFoundException("Cobrança via boleto não encontrada."));


        TransactionModel transaction = boletoPaymentDetail.getTransaction();

        // Verifica se a transação está pendente
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = transaction.getSender();
        UserModel receiver = transaction.getReceiver();
        BigDecimal amount = transaction.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        // Realiza o pagamento
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        // Atualiza a transação
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setFinalDate(LocalDateTime.now());
        transaction.setPaymentType("BOLETO");
        transaction.setPaymentDetail(boletoPaymentDetail);
        transactionRepository.save(transaction);

        return transaction;
    }

     // Atualiza a transação - NO BOLETO
        transaction.setSender(sender);OK
        transaction.setReceiver(receiver); PIX NAO
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setFinalDate(LocalDateTime.now());
        transaction.setPaymentType("BOLETO");
        transaction.setPaymentDetail(boletoPaymentDetail);
        transactionRepository.save(transaction);



        // Atualiza a transação - NO PIX
        transaction.setSender(sender);OK
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setFinalDate(LocalDateTime.now());
        transaction.setMercadoPagoPaymentId(mercadoPagoPaymentId);
        transactionRepository.save(transaction);

    //


}

Geração de boleto:

 // Salvar na tabela de transação as informações
        TransactionModel transaction = new TransactionModel();
        transaction.setSender(userModelSender); ok mas nao deixo como null
        transaction.setReceiver(userModelReceiver); ok
        transaction.setAmount(dto.amount()); ok
        transaction.setCreateDate(LocalDateTime.now()); ok
        transaction.setFinalDate(null); ok
        transaction.setStatus(TransactionStatus.PENDING); ok
        transaction.setPaymentType("BOLETO");


        //salvar na tabela de Boleto as informações
        BoletoPaymentDetail boletoPaymentDetail = new BoletoPaymentDetail();

        boletoPaymentDetail.setBoletoCode(linhaDigitavel);
        boletoPaymentDetail.setDocumentCode(numeroDocumento);

        boletoPaymentDetail.setOurNumber(nossoNumero);

        //transformando o vencimento que está em String para LocalDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate venc = LocalDate.parse(vencimento, formatter);
        boletoPaymentDetail.setDueDate(venc);
        boletoPaymentDetail.setTransaction(transaction);


        //mudei aqui
       // transaction.setPaymentDetail(boletoPaymentDetail); ok
        transaction = transactionRepository.save(transaction); ok


-----------------------------------------------------------------------------

// Cria transação PENDING (sem pagador ainda)
        TransactionModel transaction = new TransactionModel();
        transaction.setSender(null); ok mas aqui fica como null
        transaction.setReceiver(receiver); ok
        transaction.setAmount(amount); ok
        transaction.setCreateDate(LocalDateTime.now()); ok
        transaction.setFinalDate(null); ok
        transaction.setStatus(TransactionStatus.PENDING); ok

        ((((aqui eu não vejo o transaction.setPaymentType....))))

        transaction = transactionRepository.save(transaction);

        // Salva os detalhes Pix
        PixPaymentDetail pixDetail = new PixPaymentDetail();
        pixDetail.setQrCodeBase64(paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        pixDetail.setQrCodeCopyPaste(paymentResponse.getPointOfInteraction().getTransactionData().getQrCode());
        pixDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        pixDetail.setAmount(amount);
        pixDetail.setTransaction(transaction);

        // Salva o PixPaymentDetail no banco de dados
        pixPaymentDetailRepository.save(pixDetail);

        // Atualiza a transação com o PixDetail
        transaction.setPaymentDetail(pixDetail);
        transactionRepository.save(transaction);



  *********************************************************************

  1° NAO VEJO ONDE NA TRANSAÇÃO DO PIX É COLOCADO:
  transaction.setPaymentType("PIX)".......

*/