package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.PixDTOs.PixPaymentWithCreditCardRequest;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixPaymentWithCreditCardResponse;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.Enums.PaymentType;
import com.grupo5.payment_platform.Models.Payments.CreditCardPaymentDetail;
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
import com.grupo5.payment_platform.Repositories.CreditCardPaymentDetailRepository;
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

@Service
public class TransactionService {

    private TransactionRepository transactionRepository;
    private UserService userService;
    private PixPaymentDetailRepository pixPaymentDetailRepository;
    private CreditCardPaymentDetailRepository creditCardPaymentDetailRepository;


    // Injeção de dependências via construtor
    public TransactionService(TransactionRepository transactionRepository, UserService userService, PixPaymentDetailRepository pixPaymentDetailRepository,CreditCardPaymentDetailRepository creditCardPaymentDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.creditCardPaymentDetailRepository = creditCardPaymentDetailRepository;
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

        // Salva o PixPaymentDetail no banco de dados
        pixPaymentDetailRepository.save(pixDetail);

        // Atualiza a transação com o PixDetail
        transaction.setPaymentDetail(pixDetail);
        transactionRepository.save(transaction);

        return pixDetail;
    }

    // PAGAR COBRANÇA PIX
    @Transactional
    public TransactionModel pagarViaPixCopyPaste(PixSenderRequestDTO dto){

        // Busca o detalhe da cobrança pelo código Pix
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        // Pega o ID do pagamento no MercadoPago antes de verificar se pixDetail é nulo
        Long mercadoPagoPaymentId = pixDetail.getMercadoPagoPaymentId();

        // Verifica se o detalhe da cobrança existe
        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        TransactionModel transaction = pixDetail.getTransaction();

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
        BigDecimal amount = pixDetail.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        // Realiza o pagamento
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        // Atualiza a transação
        transaction.setSender(sender);
        transaction.setStatus(TransactionStatus.APPROVED);// aprovado quando pago
        transaction.setFinalDate(LocalDateTime.now());
        transaction.setMercadoPagoPaymentId(mercadoPagoPaymentId);
        transactionRepository.save(transaction);

        return transaction;
    }

    // Este metodo é chamado automaticamente pelo Spring, não manualmente
    @Scheduled(fixedRate = 60000)
    public void cancelarPixPendentes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(1);
        List<TransactionModel> pendentes = transactionRepository.findByStatusAndCreateDateBefore(TransactionStatus.PENDING, limite);
        for (TransactionModel transacao : pendentes) {
            transacao.setStatus(TransactionStatus.CANCELLED);
            transactionRepository.save(transacao);
        }
    }

    //Pagamento da cobrança pix com cartao de credito
    @Transactional
    public PixPaymentWithCreditCardResponse pagarCobrancaPixComCartao(PixPaymentWithCreditCardRequest dto) throws MPException, MPApiException {
        // Busca o detalhe da cobrança Pix
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        TransactionModel originalTransaction = pixDetail.getTransaction();

        // Verifica se a transação já foi paga ou cancelada
        if (!TransactionStatus.PENDING.equals(originalTransaction.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = userService.findById(dto.senderId());
        if (sender == null) {
            throw new UserNotFoundException("Pagador não encontrado.");
        }

        // Calcula o valor total com a taxa de 2%
        UserModel receiver = originalTransaction.getReceiver();
        BigDecimal originalAmount = originalTransaction.getAmount();
        BigDecimal tax = originalAmount.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalAmount = originalAmount.add(tax);

        // Cria a requisição de pagamento no Mercado Pago usando o token do cartão
        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(totalAmount)
                .description("Pagamento de cobrança Pix com cartão de crédito")
                .token(dto.cardToken())
                .installments(1)
                .payer(PaymentPayerRequest.builder().email(sender.getEmail()).build())
                .build();

        PaymentClient client = new PaymentClient();
        Payment paymentResponse = client.create(createRequest);

        sender.setBalance(sender.getBalance().subtract(totalAmount));
        receiver.setBalance(receiver.getBalance().add(originalAmount));

        // Atualiza a transação original
        originalTransaction.setSender(sender);
        originalTransaction.setStatus(TransactionStatus.APPROVED);
        originalTransaction.setFinalDate(LocalDateTime.now());
        originalTransaction.setMercadoPagoPaymentId(paymentResponse.getId());
        originalTransaction.setPaymentType(PaymentType.CREDIT_CARD);

        transactionRepository.save(originalTransaction);

        // Cria e salva os detalhes do pagamento com cartão
        CreditCardPaymentDetail creditCardDetail = new CreditCardPaymentDetail();
        creditCardDetail.setMercadoPagoPaymentId(paymentResponse.getId());
        creditCardDetail.setCardLastFourDigits(paymentResponse.getCard().getLastFourDigits());
        creditCardDetail.setCardBrand(paymentResponse.getPaymentMethodId());
        creditCardDetail.setInstallments(paymentResponse.getInstallments());
        creditCardDetail.setTransaction(originalTransaction);

        creditCardPaymentDetailRepository.save(creditCardDetail);

        // Retorna o DTO de resposta
        return new PixPaymentWithCreditCardResponse(
                dto.qrCodeCopyPaste(),
                originalTransaction.getStatus().name(),
                totalAmount,
                PaymentType.CREDIT_CARD.name()
        );
    }
}
