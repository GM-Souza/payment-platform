package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.PixDTOs.DepositRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.WithdrawRequestDTO;
import com.grupo5.payment_platform.Models.Payments.PixModel;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.Exceptions.*;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Repositories.BoletoRepository;
import com.grupo5.payment_platform.Repositories.PixPaymentDetailRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.grupo5.payment_platform.Repositories.UserRepository;
import com.grupo5.payment_platform.Services.UsersServices.UserService;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserService userService;
    private UserRepository userRepository;
    private PixPaymentDetailRepository pixPaymentDetailRepository;
    private BoletoRepository boletoRepository;


    public TransactionService(TransactionRepository transactionRepository, BoletoRepository boletoRepository, PixPaymentDetailRepository pixPaymentDetailRepository, UserRepository userRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.boletoRepository = boletoRepository;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }


    public TransactionModel depositFunds(DepositRequestDTO dto) {
        UserModel user = userService.findById(dto.userId());
        if (user == null) {
            throw new UserNotFoundException("Usuário não encontrado");
        }
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor do depósito deve ser maior que zero.");
        }

        user.setBalance(user.getBalance().add(dto.amount()));
        userRepository.save(user); // Salva o usuario com saldo atualizado

        TransactionModel depositTransaction = new TransactionModel();
        depositTransaction.setUser(user);
        depositTransaction.setAmount(dto.amount());
        depositTransaction.setDate(LocalDateTime.now());
        depositTransaction.setStatus(TransactionStatus.APPROVED);
        depositTransaction.setPaymentType("DEPOSIT");

        transactionRepository.save(depositTransaction);

        return depositTransaction;
    }

    public TransactionModel withdrawFunds(WithdrawRequestDTO dto) {
        UserModel user = userService.findById(dto.userId());
        if (user == null) {
            throw new UserNotFoundException("Usuário não encontrado");
        }
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor do saque deve ser maior que zero.");
        }
        if (user.getBalance().compareTo(dto.amount()) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente para saque.");
        }

        user.setBalance(user.getBalance().subtract(dto.amount()));
        userRepository.save(user); // Service delega para o Repository

        TransactionModel withdrawTransaction = new TransactionModel();
        withdrawTransaction.setUser(user);
        withdrawTransaction.setAmount(dto.amount());
        withdrawTransaction.setDate(LocalDateTime.now());
        withdrawTransaction.setStatus(TransactionStatus.APPROVED);
        withdrawTransaction.setPaymentType("WITHDRAW");

        transactionRepository.save(withdrawTransaction);

        return withdrawTransaction;
    }

    // CRIAR TRANSAÇÃO SIMPLES
    public TransactionModel createTransaction(TransactionRequestDTO dto){

        UserModel sender = userService.findById(dto.senderId());
      if (sender == null) {
          throw new UserNotFoundException("Sender não encontrado");
      }

      UserModel receiver = userService.findById(dto.receiverId());
      if (receiver == null) {
          throw new UserNotFoundException("Receiver não encontrado");
      }

        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("O valor da transação deve ser maior que zero.");
      }

        if (sender.getBalance().compareTo(dto.amount()) < 0) {
            throw new InsufficientBalanceException("O Remetente não tem saldo suficiente.");
        }

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionAmountException("O remetente e o destinatário não podem ser o mesmo usuário.");
        }
        sender.setBalance(sender.getBalance().subtract(dto.amount()));
        receiver.setBalance(receiver.getBalance().add(dto.amount()));

        TransactionModel newTransaction = new TransactionModel();
        // newTransaction.setSender(sender);
       // newTransaction.setReceiver(receiver);
        newTransaction.setAmount(dto.amount());
       //newTransaction.setCreateDate(LocalDateTime.now());
       // newTransaction.setFinalDate(null);
        newTransaction.setPaymentType("PIX");
        transactionRepository.save(newTransaction);

        return newTransaction;
    }

    // GERAR COBRANÇA PIX
    @Transactional
    public PixPaymentDetail gerarCobrancaPix(PixReceiverRequestDTO detail) throws MPException, MPApiException {

        // Verifica se o receiver existe
        UserModel receiver = userService.findById(detail.receiverId());
        BigDecimal amount = detail.amount();

        // Valida o valor
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

        // Inicializa o cliente do MercadoPago
        PaymentClient client = new PaymentClient();

        // Cria o pagamento
        Payment paymentResponse = client.create(createRequest);

        // Cria transação PENDING (sem pagador ainda)
        PixModel transaction = new PixModel();
        transaction.setReceiver(receiver);
        transaction.setUser(receiver);
        transaction.setAmount(amount);
        transaction.setDate(LocalDateTime.now());
        transaction.setPaymentType("PIX");
        transaction.setStatus(TransactionStatus.PENDING);

        PixPaymentDetail pixDetail = new PixPaymentDetail();
        pixDetail.setQrCodeBase64(paymentResponse.getPointOfInteraction().getTransactionData().getQrCodeBase64());
        pixDetail.setQrCodeCopyPaste(paymentResponse.getPointOfInteraction().getTransactionData().getQrCode());
        pixDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        transaction.attachDetail(pixDetail); // vincula ambos os lados

        transactionRepository.save(transaction); // cascade salva o detail

        return pixDetail;
    }

    // PAGAR COBRANÇA PIX
    @Transactional
    public PixModel pagarViaPixCopyPaste(PixSenderRequestDTO dto){

        // Busca o detalhe da cobrança pelo código Pix
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        // Verifica se o detalhe da cobrança existe
        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        PixModel transaction = pixDetail.getPixTransaction();

        // Verifica se a transação está pendente
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = userService.findById(dto.senderId());

        // Verifica se o pagador existe
        if (sender == null) {
            throw new UserNotFoundException("Pagador não encontrado.");
        }


        UserModel receiver = transaction.getReceiver();
        BigDecimal amount = transaction.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        // Realiza o pagamento
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        // Atualiza a transação
        transaction.setUser(sender);
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setFinalDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        return transaction;
    }

    // Este metodo é chamado automaticamente pelo Spring, não manualmente
    @Scheduled(fixedRate = 60000)
    public void cancelarPixPendentes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(1);
        List<TransactionModel> pendentes = transactionRepository.findByStatusAndDateBefore(TransactionStatus.PENDING, limite);
        for (TransactionModel transacao : pendentes) {
            transacao.setStatus(TransactionStatus.CANCELLED);
            transactionRepository.save(transacao);
        }
    }

    public List<String> listAllTransactions(UUID userId){
        List<TransactionModel> transactions = transactionRepository.findBySenderIdOrReceiverId(userId, userId);
        return transactions.stream()
                .map(tx -> {
                    String sinal = tx.getSender() != null && tx.getSender().getId().equals(userId) ? "-" : "+";
                    return sinal + " " + tx.getAmount() + " | " + tx.getPaymentType();
                })
                .toList();
    }

    // PAGAR BOLETO
    @Transactional
    public TransactionModel pagarViaCodigoBoleto(PagBoletoRequestDTO dto){

        BoletoPaymentDetail boletoPaymentDetail = boletoRepository.findByBoletoCode(dto.codeBoleto()).orElseThrow(()->
                new CodeBoletoNotFoundException("Cobrança via boleto não encontrada."));


        TransactionModel transaction = boletoPaymentDetail.getTransaction();

        // Verifica se a transação está pendente
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            transaction.setFinalDate(LocalDateTime.now());
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

}
