package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.InsufficientBalanceException;
import com.grupo5.payment_platform.Exceptions.InvalidTransactionAmountException;
import com.grupo5.payment_platform.Exceptions.PixQrCodeNotFoundException;
import com.grupo5.payment_platform.Exceptions.UserNotFoundException;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Repositories.PixPaymentDetailRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.grupo5.payment_platform.Services.UsersServices.UserService;
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

    private TransactionRepository transactionRepository;
    private UserService userService;
    private PixPaymentDetailRepository pixPaymentDetailRepository;


    public TransactionService(TransactionRepository transactionRepository, UserService userService, PixPaymentDetailRepository pixPaymentDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
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
        newTransaction.setCreateDate(LocalDateTime.now());
        newTransaction.setFinalDate(null);
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
        transaction.setCreateDate(LocalDateTime.now());
        transaction.setFinalDate(null);
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
        transaction.setFinalDate(LocalDateTime.now());
        transaction.setMercadoPagoPaymentId(mercadoPagoPaymentId);
        transactionRepository.save(transaction);

        return transaction;
    }
}
