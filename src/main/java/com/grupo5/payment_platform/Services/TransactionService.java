package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.InvalidTransactionAmountException;
import com.grupo5.payment_platform.Exceptions.InsufficientBalanceException;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.UserModel;
import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserService userService;


    public TransactionService (TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }
    //Metodo de transferencia interna p2p
    @Transactional
    public TransactionModel createTransaction(TransactionRequestDTO dto){

        UserModel sender = this.userService.findById(dto.senderId());
        UserModel receiver = this.userService.findById(dto.receiverId());

        // validações
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("The transaction amount must be greater than zero.");
        }

        if (sender.getBalance().compareTo(dto.amount()) < 0) {
            throw new InsufficientBalanceException("Sender does not have enough balance.");
        }

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionAmountException("Sender and receiver cannot be the same user.");
        }
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

    public TransactionModel createPixTransaction(TransactionRequestDTO dto) throws Exception {
        UserModel sender = this.userService.findById(dto.senderId());
        UserModel receiver = this.userService.findById(dto.receiverId());

        // validações
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("The transaction amount must be greater than zero.");
        }

        if (sender.getBalance().compareTo(dto.amount()) < 0) {
            throw new InsufficientBalanceException("Sender does not have enough balance.");
        }

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionAmountException("Sender and receiver cannot be the same user.");
        }

        TransactionModel pixTransaction = new TransactionModel();
        pixTransaction.setSender(sender);
        pixTransaction.setReceiver(receiver);
        pixTransaction.setAmount(dto.amount());
        pixTransaction.setDate(LocalDateTime.now());
        pixTransaction.setStatus(TransactionStatus.PENDING);

        sender.setBalance(sender.getBalance().subtract(dto.amount()));
        receiver.setBalance(receiver.getBalance().add(dto.amount()));

        PixPaymentDetail pixPaymentDetail = new PixPaymentDetail();
        pixPaymentDetail.setTransaction(pixTransaction);
        pixTransaction.setPaymentDetail(pixPaymentDetail);

        try{
            PaymentCreateRequest createRequest =
                    PaymentCreateRequest.builder()
                            .transactionAmount(pixTransaction.getAmount())
                            .description("Payment from " + sender.getEmail() + " to " + receiver.getEmail())
                            .paymentMethodId("pix")
                            .payer(PaymentPayerRequest.builder()
                            .email(sender.getEmail())
                                    .build())
                            .build();

            PaymentClient client = new PaymentClient();
            Payment paymentResponse = client.create(createRequest);

            pixTransaction.setMercadoPagoPaymentId(paymentResponse.getId());
            PixPaymentDetail savedPixDetails = (PixPaymentDetail) pixTransaction.getPaymentDetail();

            //Metodos para obter o qrcode e o codigo do qrcode referente a transação
            String base64Image = paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64();
            savedPixDetails.setQrCodeBase64(base64Image);

            String copyPasteCode = paymentResponse.getPointOfInteraction().getTransactionData().getQrCode();
            savedPixDetails.setQrCodeCopyPaste(copyPasteCode);
            pixTransaction.setStatus(TransactionStatus.APPROVED);
            return transactionRepository.save(pixTransaction);

        }catch (MPApiException e){
            pixTransaction.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(pixTransaction);
            throw new RuntimeException("Error creating Pix transaction",e);
        }catch (MPException e){
            pixTransaction.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(pixTransaction);
            throw new RuntimeException("Error on MercadoPago SDK",e);
        }

    }
}
