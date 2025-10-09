package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.PixDTOs.DepositRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.WithdrawRequestDTO;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.*;

import com.grupo5.payment_platform.Models.Payments.BoletoModel;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.PixModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO; // import para manter o CreateTransaction
import com.grupo5.payment_platform.Repositories.*;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PixPaymentDetailRepository pixPaymentDetailRepository;
    private final BoletoRepository boletoRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditInvoiceRepository creditInvoiceRepository;

    public TransactionService(TransactionRepository transactionRepository, UserService userService,
                              UserRepository userRepository, PixPaymentDetailRepository pixPaymentDetailRepository,
                              BoletoRepository boletoRepository, CreditCardRepository creditCardRepository,
                              CreditInvoiceRepository creditInvoiceRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.boletoRepository = boletoRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditInvoiceRepository = creditInvoiceRepository;
    }

    public TransactionModel depositFunds(DepositRequestDTO dto) {
        UserModel user = userService.findByEmail(dto.email()).orElseThrow();
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
        UserModel user = userService.findByEmail(dto.email()).orElseThrow();

        if (user.getEmail() == null) {
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
      if (sender.getEmail() == null) {
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
        UserModel receiver = userService.findByEmail(detail.receiverEmail()).orElseThrow();

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

        UserModel sender = userService.findByEmail(dto.senderEmail()).orElseThrow();

        // Verifica se o pagador existe
        if (sender.getEmail() == null) {
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

    @Transactional
    public BoletoModel pagarViaCodigoBoleto(PagBoletoRequestDTO dto) {
        BoletoPaymentDetail boletoPaymentDetail = boletoRepository.findByBoletoCode(dto.codeBoleto())
                .orElseThrow(() -> new CodeBoletoNotFoundException("Cobrança via boleto não encontrada."));
        BoletoModel boletoTx = boletoPaymentDetail.getBoletoTransaction();
        if (!boletoTx.isPending()) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        UserModel sender = boletoTx.getSender();
        UserModel receiver = boletoTx.getReceiver();
        BigDecimal amount = boletoTx.getAmount();

        if (sender.getBalance().compareTo(amount) < 0)
            throw new InsufficientBalanceException("Saldo insuficiente.");

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        boletoTx.setStatus(TransactionStatus.APPROVED);
        boletoTx.setFinalDate(LocalDateTime.now());

        transactionRepository.save(boletoTx);

        return boletoTx;
    }



    @Scheduled(cron = "0 0 2 27 * ?") // todo dia 27 às 02:00 cria uma nova fatura
    public void generateMonthlyInvoices() {
        List<CreditCardModel> cards = creditCardRepository.findAll();

        for (CreditCardModel card : cards) {
            CreditInvoiceModel invoice = new CreditInvoiceModel();
            invoice.setCreditCardId(card);

            LocalDate closingDate = LocalDate.now().withDayOfMonth(27);
            LocalDate dueDate = closingDate.plusMonths(1).withDayOfMonth(5);

            invoice.setClosingDate(closingDate);
            invoice.setDueDate(dueDate);
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setPaid(false);

            creditInvoiceRepository.save(invoice);
        }
    }

    @Transactional
    public BoletoModel pagarBoletoViaCreditCard(PagBoletoRequestDTO dto, int parcelas) {
        // Busca o boleto
        BoletoPaymentDetail boletoPaymentDetail = boletoRepository.findByBoletoCode(dto.codeBoleto())
                .orElseThrow(() -> new CodeBoletoNotFoundException("Cobrança via boleto não encontrada."));

        BoletoModel boletoTx = boletoPaymentDetail.getBoletoTransaction();

        if (!boletoTx.isPending()) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        BigDecimal amount = boletoTx.getAmount();
        BigDecimal valorParcela = amount.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

        // Busca o cartão do usuário via repositório
        CreditCardModel card = creditCardRepository.findByUserOwnerId(boletoTx.getSender())
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado para o usuário."));

        // Busca faturas existentes, ordenadas pelo fechamento
        List<CreditInvoiceModel> invoices = creditInvoiceRepository.findByCreditCardIdOrderByClosingDateAsc(card);

        LocalDate now = LocalDate.now();

        // Filtra faturas futuras (closingDate >= now) e ordena por closingDate asc
        List<CreditInvoiceModel> futureInvoices = invoices.stream()
                .filter(inv -> !inv.getClosingDate().isBefore(now))
                .sorted(Comparator.comparing(CreditInvoiceModel::getClosingDate))
                .collect(Collectors.toList());

        // Cria faturas futuras automaticamente se não houver faturas suficientes
        while (futureInvoices.size() < parcelas) {
            LocalDate lastClosingDate = futureInvoices.isEmpty() ? now.withDayOfMonth(27)
                    : futureInvoices.get(futureInvoices.size() - 1).getClosingDate();
            LocalDate newClosingDate = lastClosingDate.plusMonths(1);
            LocalDate newDueDate = newClosingDate.plusMonths(1).withDayOfMonth(5);

            CreditInvoiceModel newInvoice = new CreditInvoiceModel();
            newInvoice.setCreditCardId(card);
            newInvoice.setClosingDate(newClosingDate);
            newInvoice.setDueDate(newDueDate);
            newInvoice.setTotalAmount(BigDecimal.ZERO);
            newInvoice.setPaid(false);

            creditInvoiceRepository.save(newInvoice);
            futureInvoices.add(newInvoice);
        }

        // Distribui cada parcela em uma fatura futura distinta
        for (int i = 0; i < parcelas; i++) {
            CreditInvoiceModel invoice = futureInvoices.get(i);
            invoice.setTotalAmount(invoice.getTotalAmount().add(valorParcela));
            creditInvoiceRepository.save(invoice);
        }

        // Atualiza status do boleto
        boletoTx.setStatus(TransactionStatus.APPROVED);
        boletoTx.setFinalDate(LocalDateTime.now());
        transactionRepository.save(boletoTx);

        // Atualiza saldo do recebedor
        UserModel receiver = boletoTx.getReceiver();
        receiver.setBalance(receiver.getBalance().add(amount));

        return boletoTx;
    }

    @Transactional
    public PixModel pagarPixViaCreditCard(PixSenderRequestDTO dto, int parcelas) {
        // Busca o detalhe da cobrança PIX pelo código copy-paste
        PixPaymentDetail pixPaymentDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        // Verifica se o detalhe da cobrança existe
        if (pixPaymentDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        PixModel pixTx = pixPaymentDetail.getPixTransaction();

        // Verifica se a transação está pendente
        if (!TransactionStatus.PENDING.equals(pixTx.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        BigDecimal amount = pixTx.getAmount();
        BigDecimal valorParcela = amount.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

        // Busca o usuário pagador pelo senderEmail do DTO
        UserModel sender = userService.findByEmail(dto.senderEmail()).orElseThrow();

        if (sender == null) {
            throw new UserNotFoundException("Pagador não encontrado.");
        }

        // Busca o cartão do usuário pagador via repositório
        CreditCardModel card = creditCardRepository.findByUserOwnerId(sender)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado para o usuário."));

        // Busca faturas existentes, ordenadas pelo fechamento
        List<CreditInvoiceModel> invoices = creditInvoiceRepository.findByCreditCardIdOrderByClosingDateAsc(card);

        LocalDate now = LocalDate.now();

        // Filtra faturas futuras (closingDate >= now) e ordena por closingDate asc
        List<CreditInvoiceModel> futureInvoices = invoices.stream()
                .filter(inv -> !inv.getClosingDate().isBefore(now))
                .sorted(Comparator.comparing(CreditInvoiceModel::getClosingDate))
                .collect(Collectors.toList());

        // Cria faturas futuras automaticamente se não houver faturas suficientes
        while (futureInvoices.size() < parcelas) {
            LocalDate lastClosingDate = futureInvoices.isEmpty() ? now.withDayOfMonth(27)
                    : futureInvoices.get(futureInvoices.size() - 1).getClosingDate();
            LocalDate newClosingDate = lastClosingDate.plusMonths(1);
            LocalDate newDueDate = newClosingDate.plusMonths(1).withDayOfMonth(5);

            CreditInvoiceModel newInvoice = new CreditInvoiceModel();
            newInvoice.setCreditCardId(card);
            newInvoice.setClosingDate(newClosingDate);
            newInvoice.setDueDate(newDueDate);
            newInvoice.setTotalAmount(BigDecimal.ZERO);
            newInvoice.setPaid(false);

            creditInvoiceRepository.save(newInvoice);
            futureInvoices.add(newInvoice);
        }

        // Distribui cada parcela em uma fatura futura distinta
        for (int i = 0; i < parcelas; i++) {
            CreditInvoiceModel invoice = futureInvoices.get(i);
            invoice.setTotalAmount(invoice.getTotalAmount().add(valorParcela));
            creditInvoiceRepository.save(invoice);
        }

        // Atualiza status da transação PIX
        pixTx.setStatus(TransactionStatus.APPROVED);
        pixTx.setFinalDate(LocalDateTime.now());
        pixTx.setUser(sender); // Seta o pagador como user da transação
        transactionRepository.save(pixTx);

        // Atualiza saldo do recebedor
        UserModel receiver = pixTx.getReceiver();
        receiver.setBalance(receiver.getBalance().add(amount));

        return pixTx;
    }
}
