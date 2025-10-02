package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.TransactionRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.InsufficientBalanceException;
import com.grupo5.payment_platform.Exceptions.InvalidTransactionAmountException;
import com.grupo5.payment_platform.Exceptions.PixQrCodeNotFoundException;
import com.grupo5.payment_platform.Exceptions.UserNotFoundException;
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

    //Metodo de pagamento pix
  @Transactional
    public TransactionModel createPixTransaction(TransactionRequestDTO dto){

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


    //Metodo cobrança pix
    public PixPaymentDetail gerarCobrancaPix(TransactionRequestDTO detail) throws Exception {
        UserModel sender = userService.findById(detail.senderId());
        UserModel receiver = userService.findById(detail.receiverId());
        BigDecimal amount = detail.amount();

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

        TransactionModel transaction = new TransactionModel();
        transaction.setSender(null); // ainda não tem pagador
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);

        transaction = transactionRepository.save(transaction);

        PixPaymentDetail pixDetail = new PixPaymentDetail();
        pixDetail.setQrCodeBase64(paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        pixDetail.setQrCodeCopyPaste(paymentResponse.getPointOfInteraction().getTransactionData().getQrCode());
        pixDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        pixDetail.setAmount(amount);
        pixDetail.setTransaction(transaction);

        pixPaymentDetailRepository.save(pixDetail); // Salva o Pix detail

        // 4. (Opcional) Atualiza a transação com o PixDetail
        transaction.setPaymentDetail(pixDetail);
        transactionRepository.save(transaction);

        return pixDetail;
    }

    //Metodo para pagar a cobrança pix
    @Transactional
    public TransactionModel pagarViaPixCopyPaste(UUID uuid, String qrCodeCopyPaste) throws Exception {

        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(qrCodeCopyPaste);
        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }
        UserModel sender = pixDetail.getTransaction().getSender();


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
}
