package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.InsufficientBalanceException;
import com.grupo5.payment_platform.Exceptions.InvalidTransactionAmountException;
import com.grupo5.payment_platform.Exceptions.PixQrCodeNotFoundException;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.UserModel;
import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
import com.grupo5.payment_platform.Repositories.PixPaymentDetailRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserService userService;
    private PixPaymentDetailRepository pixPaymentDetailRepository;


    public TransactionService(TransactionRepository transactionRepository, UserService userService, PixPaymentDetailRepository pixPaymentDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
    }

    //Metodo de transferencia pix
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

    //Alterações no pagamento via pix com mercado pago

    // Cria a cobrança Pix e retorna os dados para pagamento
    public PixPaymentDetail gerarCobrancaPix(UUID receiverId, BigDecimal amount) throws Exception {
        UserModel receiver = userService.findById(receiverId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor deve ser maior que zero.");
        }

        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(amount)
                .description("Cobrança Pix para " + receiver.getEmail())
                .paymentMethodId("pix")
                .payer(PaymentPayerRequest.builder().email(receiver.getEmail()).build())
                .build();

        PaymentClient client = new PaymentClient();
        Payment paymentResponse = client.create(createRequest);

        PixPaymentDetail pixDetail = new PixPaymentDetail();
        pixDetail.setQrCodeBase64(paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        pixDetail.setQrCodeCopyPaste(paymentResponse.getPointOfInteraction().getTransactionData().getQrCode());
        pixDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        pixDetail.setAmount(amount);

        // Salva cobrança no banco (status PENDING)
        TransactionModel transaction = new TransactionModel();
        transaction.setSender(null); // ainda não tem pagador
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setPaymentDetail(pixDetail);

        transactionRepository.save(transaction);

        return pixDetail;
    }

    //Metodo para pagar o pix via copy-paste
    @Transactional
    public TransactionModel pagarViaPixCopyPaste(UUID senderId, String qrCodeCopyPaste) throws Exception {
        UserModel sender = userService.findById(senderId);

        // Busca cobrança pelo código copy-paste
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(qrCodeCopyPaste);
        if (pixDetail == null) {
            throw new RuntimeException("Cobrança Pix não encontrada.");
        }

        // Verifica se o pagamento foi aprovado via MercadoPago
        PaymentClient client = new PaymentClient();
        Payment payment = client.get(pixDetail.getMercadoPagoPaymentId());
        if (!"approved".equalsIgnoreCase(payment.getStatus())) {
            throw new RuntimeException("Pagamento Pix não aprovado.");
        }

        UserModel receiver = pixDetail.getTransaction().getReceiver();
        BigDecimal amount = pixDetail.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        TransactionModel transaction = pixDetail.getTransaction();
        transaction.setSender(sender);
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);

        return transaction;
    }
    //Metodo para pagar o pix via copy-paste sem mercado pago (simulação)
    public TransactionModel CopyPastTransaction(String qrCodeCopyPaste){
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(qrCodeCopyPaste);
        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }
        var transaction = pixDetail.getTransaction();
        transaction.getReceiver().setBalance(transaction.getReceiver().getBalance().add(pixDetail.getAmount()));
        transaction.getSender().setBalance(transaction.getSender().getBalance().subtract(pixDetail.getAmount()));
        transaction.setStatus(TransactionStatus.APPROVED);
        return transactionRepository.save(transaction);
    }

}
