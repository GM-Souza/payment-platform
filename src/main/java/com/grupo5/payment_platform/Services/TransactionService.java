package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.PaymentsDTO.*;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.*;
import com.grupo5.payment_platform.Models.TransactionModel;
import com.grupo5.payment_platform.Models.UserModel;
import com.grupo5.payment_platform.Models.payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
import com.grupo5.payment_platform.Repositories.BoletoPaymentDetailRepository;
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

@Service
public class TransactionService {

    private final BoletoPaymentDetailRepository boletoPaymentDetailRepository;
    private TransactionRepository transactionRepository;
    private UserService userService;
    private PixPaymentDetailRepository pixPaymentDetailRepository;


    public TransactionService(TransactionRepository transactionRepository, UserService userService, PixPaymentDetailRepository pixPaymentDetailRepository, BoletoPaymentDetailRepository boletoPaymentDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.boletoPaymentDetailRepository = boletoPaymentDetailRepository;
    }

    public TransactionModel createTransaction(TransactionRequestDTO dto){

        UserModel sender = userService.findById(dto.senderId());
      if (sender == null) {
          throw new UserNotFoundException("Receiver não encontrado");
      }

      UserModel receiver = userService.findById(dto.receiverId());
      if (receiver == null) {
          throw new UserNotFoundException("Receiver não encontrado");
      }

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



    // GERAR COBRANÇA PIX

    @Transactional
    public PixPaymentDetail gerarCobrancaPix(PixReceiverRequestDTO detail) throws Exception {
        UserModel receiver = userService.findById(detail.receiverId());
        BigDecimal amount = detail.amount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor deve ser maior que zero.");
        }

        // Cria cobrança no MercadoPago
        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(amount)
                .description("Cobrança Pix para " + receiver.getEmail())
                .paymentMethodId("pix")
                .payer(PaymentPayerRequest.builder().email(receiver.getEmail()).build())
                .build();

        PaymentClient client = new PaymentClient();
        Payment paymentResponse = client.create(createRequest);

        // Cria transação PENDING (sem pagador ainda)
        TransactionModel transaction = new TransactionModel();
        transaction.setSender(null);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        // Salva os detalhes Pix
        PixPaymentDetail pixDetail = new PixPaymentDetail();
        pixDetail.setQrCodeBase64(paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        pixDetail.setQrCodeCopyPaste(paymentResponse.getPointOfInteraction().getTransactionData().getQrCode());
        pixDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        pixDetail.setAmount(amount);
        pixDetail.setTransaction(transaction);

        pixPaymentDetailRepository.save(pixDetail);

        // Atualiza a transação com o PixDetail
        transaction.setPaymentDetail(pixDetail);
        transactionRepository.save(transaction);

        return pixDetail;
    }

    // PAGAR COBRANÇA PIX

    @Transactional
    public TransactionModel pagarViaPixCopyPaste(PixSenderRequestDTO dto){
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());
        Long mercadoPagoPaymentId = pixDetail.getMercadoPagoPaymentId();
        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        TransactionModel transaction = pixDetail.getTransaction();

        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = userService.findById(dto.senderId());
        if (sender == null) {
            throw new UserNotFoundException("Pagador não encontrado.");
        }

        UserModel receiver = transaction.getReceiver();
        BigDecimal amount = pixDetail.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        // Realiza o pagamento
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        transaction.setSender(sender);
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setMercadoPagoPaymentId(mercadoPagoPaymentId);
        transactionRepository.save(transaction);

        return transaction;
    }
    // GERAR COBRANÇA BOLETO
    public BoletoPaymentDetail gerarCobrancaBoleto(BoletoReceiverRequestDTO detail) throws Exception {
        UserModel receiver = userService.findById(detail.receiverId());
        BigDecimal amount = detail.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor deve ser maior que zero.");
        }
        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(amount)
                .description("Cobrança Boleto para " + receiver.getEmail())
                .paymentMethodId("bolbradesco")
                .payer(PaymentPayerRequest.builder().email(receiver.getEmail()).build())
                .build();

        PaymentClient client = new PaymentClient();
        Payment paymentResponse = client.create(createRequest);

        TransactionModel transaction = new TransactionModel();
        transaction.setSender(null);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        BoletoPaymentDetail boletoDetail = new BoletoPaymentDetail();
        boletoDetail.setBarcode(paymentResponse.getCouponCode());
        boletoDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        boletoDetail.setAmount(amount);
        boletoDetail.setTransaction(transaction);

        boletoPaymentDetailRepository.save(boletoDetail);

        transaction.setPaymentDetail(boletoDetail);
        transactionRepository.save(transaction);
        return boletoDetail;
    }

    // PAGAR COBRANÇA BOLETO

    @Transactional
    public TransactionModel pagarCobrancaBoleto(BoletoSenderRequestDTO detail) throws Exception {
        BoletoPaymentDetail boletoDetail = boletoPaymentDetailRepository.findByBarcode(detail.barcode());
        if (boletoDetail == null) {
            throw new BoletoBarcodeNotFoundException("Cobrança Boleto não encontrada.");
        }
        TransactionModel transaction = boletoDetail.getTransaction();

        if (!transaction.getStatus().equals(TransactionStatus.PENDING)) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = userService.findById(detail.senderId());
        if (sender == null || sender == boletoDetail.getTransaction().getReceiver()) {
            throw new UserNotFoundException("Pagador não encontrado ou inválido.");
        }

        BigDecimal amount = boletoDetail.getAmount();
        if(sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        transaction.getReceiver().setBalance(transaction.getAmount().add(amount));

        transaction.setSender(sender);
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setMercadoPagoPaymentId(boletoDetail.getMercadoPagoPaymentId());
        transactionRepository.save(transaction);

        return transaction;
    }
}
