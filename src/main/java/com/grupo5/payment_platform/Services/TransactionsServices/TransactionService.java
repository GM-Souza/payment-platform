package com.grupo5.payment_platform.Services.TransactionsServices;

import com.grupo5.payment_platform.DTOs.Cards.CreditCardRequestDTO;
import com.grupo5.payment_platform.DTOs.Cards.PagCreditCardRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.DepositRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.WithdrawRequestDTO;
import com.grupo5.payment_platform.DTOs.BoletosDTOs.PagBoletoRequestDTO;
import com.grupo5.payment_platform.Enums.EmailSubject;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.*;

import com.grupo5.payment_platform.Infra.Kafka.TransactionNotificationDTO;
import com.grupo5.payment_platform.Models.Payments.BoletoModel;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.PixModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.IndividualModel;
import com.grupo5.payment_platform.Models.Users.LegalEntityModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Models.card.ParcelModel;
import com.grupo5.payment_platform.Obsolete.TransactionRequestDTO; // import para manter o CreateTransaction
import com.grupo5.payment_platform.Repositories.*;

import com.grupo5.payment_platform.Services.TransactionKafkaService;
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
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PixPaymentDetailRepository pixPaymentDetailRepository;
    private final BoletoRepository boletoRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditInvoiceRepository creditInvoiceRepository;
    private final ParcelRepository parcelRepository;
    private final TransactionKafkaService transactionKafkaService;

    public TransactionService(TransactionRepository transactionRepository, UserService userService,
                              UserRepository userRepository, PixPaymentDetailRepository pixPaymentDetailRepository,
                              BoletoRepository boletoRepository, CreditCardRepository creditCardRepository,
                              CreditInvoiceRepository creditInvoiceRepository, ParcelRepository parcelRepository, TransactionKafkaService transactionKafkaService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.boletoRepository = boletoRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditInvoiceRepository = creditInvoiceRepository;
        this.parcelRepository = parcelRepository;
        this.transactionKafkaService = transactionKafkaService;
    }

    public TransactionModel depositFunds(DepositRequestDTO dto) {
        UserModel user = userService.findByEmail(dto.email())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

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

        UserModel user = userService.findByEmail(dto.email())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

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

    // CRIAR TRANSAÇÃO PIX SIMPLES
    public TransactionModel createTransaction(TransactionRequestDTO dto){

        UserModel sender = userService.findByEmail(dto.senderEmail())
                .orElseThrow(() -> new UserNotFoundException("Email do remetente não encontrado"));

      UserModel receiver = userService.findByEmail(dto.receiverEmail())
              .orElseThrow(() -> new UserNotFoundException("Email do destinatário não encontrado"));

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

        PixModel newTransaction = new PixModel();

        newTransaction.setUser(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setAmount(dto.amount());
        newTransaction.setDate(LocalDateTime.now());
        newTransaction.setFinalDate(LocalDateTime.now());
        newTransaction.setStatus(TransactionStatus.APPROVED);
        newTransaction.setPaymentType("PIX");

        transactionRepository.save(newTransaction);

        return newTransaction;
    }

    // GERAR COBRANÇA PIX
    @Transactional
    public PixPaymentDetail gerarCobrancaPix(PixReceiverRequestDTO detail) throws MPException, MPApiException {

        // Verifica se o receiver existe
        UserModel receiver = userService.findByEmail(detail.receiverEmail()).orElseThrow(() ->
                new UserNotFoundException("Usuário não encontrado."));

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
        transaction.setUser(null);
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
        TransactionNotificationDTO notifySender =
                new TransactionNotificationDTO(sender.getEmail(), sender.getEmail(), EmailSubject.PAYMENT_SUCESS);
        transactionKafkaService.sendTransactionNotificationForBoth(notifySender, null);

        TransactionNotificationDTO notifyReceiver =
                new TransactionNotificationDTO(receiver.getEmail(), receiver.getEmail(), EmailSubject.PAYMENT_RECEIVED);
        transactionKafkaService.sendTransactionNotificationForBoth(null, notifyReceiver);

        return transaction;
    }


    // Este metodo é chamado automaticamente pelo Spring, não manualmente
    @Scheduled(fixedRate = 6000000)
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

        if (amount == null || sender.getBalance() == null || sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        //alteração pra talvez modificar o front
        userRepository.save(sender);
        userRepository.save(receiver);

        boletoTx.setStatus(TransactionStatus.APPROVED);
        boletoTx.setFinalDate(LocalDateTime.now());

        transactionRepository.save(boletoTx);

        return boletoTx;
    }

    @Transactional
    public CreditCardModel createCreditCard(CreditCardRequestDTO dto) {
        UserModel userOwner = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Email not found!"));

        // Verifica se o usuário já possui um cartão de crédito
        Optional<CreditCardModel> existingCard = creditCardRepository.findByUserOwnerId(userOwner);
        if (existingCard.isPresent()) {
            throw new RuntimeException("User already has a credit card!");
        }

        CreditCardModel card = new CreditCardModel();

        // Número e CVV aleatórios (somente para simulação)
        String creditNumber = String.format("%016d", Math.abs(new Random().nextLong()) % 1_0000_0000_0000_0000L);
        String cvv = String.format("%03d", new Random().nextInt(1000));

        card.setCreditNumber(creditNumber);
        card.setCvv(cvv);
        card.setExpiration(LocalDate.now().plusYears(5));
        card.setCreditInvoice(BigDecimal.ZERO);
        card.setUserOwnerId(userOwner);

        // Define limite baseado no tipo de usuário
        if (userOwner instanceof IndividualModel individualReceiver) {
            card.setCreditLimit(BigDecimal.valueOf(3_000));
        } else if (userOwner instanceof LegalEntityModel legalReceiver) {
            card.setCreditLimit(BigDecimal.valueOf(6_000));
        } else {
            card.setCreditLimit(BigDecimal.valueOf(1_000)); // limite padrão caso outro tipo
        }

        creditCardRepository.save(card);

        // Gera faturas futuras (apenas a partir do mês atual)
        LocalDate baseClosingDate = LocalDate.now().withDayOfMonth(27);
        for (int i = 0; i < 12; i++) {
            LocalDate closingDate = baseClosingDate.plusMonths(i);
            LocalDate dueDate = closingDate.plusMonths(1).withDayOfMonth(5);

            CreditInvoiceModel invoice = new CreditInvoiceModel();
            invoice.setCreditCardId(card);
            invoice.setClosingDate(closingDate);
            invoice.setDueDate(dueDate);
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setPaid(false);

            creditInvoiceRepository.save(invoice);
            card.getInvoices().add(invoice);
            //aqui ou no return
        }

        return card;
    }

    @Scheduled(cron = "0 0 2 27 * ?") // executa dia 27 às 02:00
    @Transactional
    public void generateNextMonthInvoices() {
        List<CreditCardModel> cards = creditCardRepository.findAll();

        for (CreditCardModel card : cards) {
            CreditInvoiceModel lastInvoice = creditInvoiceRepository
                    .findTopByCreditCardIdOrderByClosingDateDesc(card)
                    .orElse(null);

            LocalDate lastClosingDate = (lastInvoice != null)
                    ? lastInvoice.getClosingDate()
                    : LocalDate.now().withDayOfMonth(27);

            LocalDate newClosingDate = lastClosingDate.plusMonths(1);
            LocalDate newDueDate = newClosingDate.plusMonths(1).withDayOfMonth(5);

            boolean alreadyExists = creditInvoiceRepository
                    .existsByCreditCardIdAndClosingDate(card, newClosingDate);

            if (!alreadyExists) {
                CreditInvoiceModel newInvoice = new CreditInvoiceModel();
                newInvoice.setCreditCardId(card);
                newInvoice.setClosingDate(newClosingDate);
                newInvoice.setDueDate(newDueDate);
                newInvoice.setTotalAmount(BigDecimal.ZERO);
                newInvoice.setPaid(false);

                creditInvoiceRepository.save(newInvoice);
                card.getInvoices().add(newInvoice);
            }
        }

        System.out.println("✅ Faturas futuras geradas com sucesso em " + LocalDate.now());
    }

    public CreditCardModel getCreditCardByEmail(CreditCardRequestDTO dto) {
        UserModel user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Email not found!"));
        return creditCardRepository.findByUserOwnerId(user)
                .orElseThrow(() -> new RuntimeException("Credit card not found for this user!"));
    }

    @Transactional
    public BoletoModel pagarBoletoViaCreditCard(PagBoletoRequestDTO dto, int parcelas) {
        // Busca o boleto pelo código
        BoletoPaymentDetail boletoPaymentDetail = boletoRepository.findByBoletoCode(dto.codeBoleto())
                .orElseThrow(() -> new CodeBoletoNotFoundException("Cobrança via boleto não encontrada."));

        BoletoModel boletoTx = boletoPaymentDetail.getBoletoTransaction();

        if (!boletoTx.isPending()) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        BigDecimal amount = boletoTx.getAmount();
        BigDecimal valorParcela = amount.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

        // Busca o cartão do usuário que vai pagar o boleto
        CreditCardModel card = creditCardRepository.findByUserOwnerId(boletoTx.getSender())
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado para o usuário."));

        // Limite mensal fixo
        BigDecimal limiteMensal = card.getCreditLimit();

        // Soma o total já faturado no mês corrente (faturas abertas)
        LocalDate now = LocalDate.now();
        BigDecimal totalMesAtual = card.getInvoices().stream()
                .filter(inv -> !inv.isPaid())
                .filter(inv -> inv.getClosingDate().getMonth().equals(now.getMonth()) &&
                        inv.getClosingDate().getYear() == now.getYear())
                .map(CreditInvoiceModel::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limiteDisponivel = limiteMensal.subtract(totalMesAtual);

        // Verifica se o valor cabe no limite disponível
        if (amount.compareTo(limiteDisponivel) > 0) {
            throw new RuntimeException("Limite de crédito insuficiente para realizar esta transação.");
        }

        // Busca faturas futuras já existentes
        List<CreditInvoiceModel> futureInvoices = creditInvoiceRepository
                .findByCreditCardIdAndClosingDateAfterOrderByClosingDateAsc(card, now);

        if (futureInvoices.size() < parcelas) {
            throw new RuntimeException("Não há faturas futuras suficientes para distribuir as parcelas.");
        }

        // Distribui o valor das parcelas e cria ParcelModel
        for (int i = 0; i < parcelas; i++) {
            CreditInvoiceModel invoice = futureInvoices.get(i);

            // Cria a parcela
            ParcelModel parcel = new ParcelModel();
            parcel.setInvoice(invoice);
            parcel.setOriginalTransaction(boletoTx); // Linka à transação original
            parcel.setParcelNumber(i + 1);
            parcel.setTotalParcels(parcelas);
            parcel.setAmount(valorParcela);
            parcel.setDescription("Parcela " + (i + 1) + " de " + parcelas + " do Boleto #" + dto.codeBoleto());

            // Adiciona à fatura (bidirecional)
            invoice.getParcels().add(parcel);
            parcelRepository.save(parcel);// Salva a parcela

            // Soma no total (mas recalculará depois)
            invoice.setTotalAmount(invoice.getTotalAmount().add(valorParcela));
            creditInvoiceRepository.save(invoice);
        }

        // Recalcula o total final para cada fatura afetada
        futureInvoices.forEach(invoice -> {
            invoice.recalculateTotalAmount();
            creditInvoiceRepository.save(invoice);
        });

        // Atualiza status e data de finalização do boleto
        boletoTx.setStatus(TransactionStatus.APPROVED);
        boletoTx.setPaymentType("BOLETO_CREDIT_CARD");
        boletoTx.setFinalDate(LocalDateTime.now());
        transactionRepository.save(boletoTx);

        // Atualiza saldo do recebedor
        UserModel receiver = boletoTx.getReceiver();
        receiver.setBalance(receiver.getBalance().add(amount));
        userRepository.save(receiver); // Assuma save

        //TODO kafka email - para o receiver

        return boletoTx;
    }

    @Transactional
    public PixModel pagarPixViaCreditCard(PixSenderRequestDTO dto, int parcelas) {
        // Busca o detalhe da cobrança PIX pelo código copy-paste
        PixPaymentDetail pixPaymentDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        if (pixPaymentDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        PixModel pixTx = pixPaymentDetail.getPixTransaction();

        if (!TransactionStatus.PENDING.equals(pixTx.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        BigDecimal amount = pixTx.getAmount();
        BigDecimal valorParcela = amount.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

        // Busca o usuário pagador
        UserModel sender = userService.findByEmail(dto.senderEmail()).orElseThrow(()->
                new UserNotFoundException("User not found"));

        // Busca o cartão do usuário
        CreditCardModel card = creditCardRepository.findByUserOwnerId(sender)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado para o usuário."));

        // Limite mensal fixo
        BigDecimal limiteMensal = card.getCreditLimit();

        // Soma o total já faturado no mês corrente (faturas não pagas do mês atual)
        LocalDate now = LocalDate.now();
        BigDecimal totalMesAtual = card.getInvoices().stream()
                .filter(inv -> !inv.isPaid())
                .filter(inv -> inv.getClosingDate().getMonth().equals(now.getMonth()) &&
                        inv.getClosingDate().getYear() == now.getYear())
                .map(CreditInvoiceModel::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limiteDisponivel = limiteMensal.subtract(totalMesAtual);

        // Verifica se o valor total da compra cabe no limite disponível
        if (amount.compareTo(limiteDisponivel) > 0) {
            throw new RuntimeException("Limite de crédito insuficiente para realizar esta transação.");
        }

        // Busca faturas futuras já existentes
        List<CreditInvoiceModel> futureInvoices = creditInvoiceRepository
                .findByCreditCardIdAndClosingDateAfterOrderByClosingDateAsc(card, now);

        if (futureInvoices.size() < parcelas) {
            throw new RuntimeException("Não há faturas futuras suficientes para distribuir as parcelas.");
        }

        // Distribui o valor das parcelas e cria ParcelModel
        for (int i = 0; i < parcelas; i++) {
            CreditInvoiceModel invoice = futureInvoices.get(i);

            // Cria a parcela
            ParcelModel parcel = new ParcelModel();
            parcel.setInvoice(invoice);
            parcel.setOriginalTransaction(pixTx); // Linka à transação original
            parcel.setParcelNumber(i + 1);
            parcel.setTotalParcels(parcelas);
            parcel.setAmount(valorParcela);
            parcel.setDescription("Parcela " + (i + 1) + " de " + parcelas + " do PIX #" + dto.qrCodeCopyPaste().substring(0, 10) + "..."); // Trunca o código pra description

            // Adiciona à fatura (bidirecional)
            invoice.getParcels().add(parcel);
            parcelRepository.save(parcel); // Salva a parcela

            // Soma no total (mas recalculará depois)
            invoice.setTotalAmount(invoice.getTotalAmount().add(valorParcela));
            creditInvoiceRepository.save(invoice);
        }

        // Recalcula o total final para cada fatura afetada
        futureInvoices.forEach(invoice -> {
            invoice.recalculateTotalAmount();
            creditInvoiceRepository.save(invoice);
        });

        // Atualiza status da transação PIX
        pixTx.setStatus(TransactionStatus.APPROVED);
        pixTx.setPaymentType("PIX_CREDIT_CARD");
        pixTx.setFinalDate(LocalDateTime.now());
        pixTx.setUser(sender);
        transactionRepository.save(pixTx);

        // Atualiza saldo do recebedor
        UserModel receiver = pixTx.getReceiver();
        receiver.setBalance(receiver.getBalance().add(amount));
        userRepository.save(receiver); // Assuma save

        //TODO kafka email - para o receiver

        return pixTx;
    }

    @Transactional
    public CreditInvoiceModel pagarProximaFatura(PagCreditCardRequestDTO dto) {
        // 1. Busca o usuário pelo e-mail
        UserModel user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Busca o cartão de crédito do usuário
        CreditCardModel card = creditCardRepository.findByUserOwnerId(user)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado"));

        // 3. Busca a próxima fatura não paga (mesmo que futura)
        CreditInvoiceModel invoice = card.getInvoices().stream()
                .filter(inv -> !inv.isPaid())
                .min(Comparator.comparing(CreditInvoiceModel::getClosingDate)) // pega a mais próxima
                .orElseThrow(() -> new RuntimeException("Não há faturas pendentes"));

        // 4. Calcula juros se estiver vencida
        LocalDate hoje = LocalDate.now();
        BigDecimal totalAPagar = invoice.getTotalAmount();
        if (hoje.isAfter(invoice.getDueDate())) {
            long mesesAtraso = ChronoUnit.MONTHS.between(
                    YearMonth.from(invoice.getDueDate()),
                    YearMonth.from(hoje)
            );
            BigDecimal taxaJurosMensal = new BigDecimal("0.01"); // 1% ao mês
            BigDecimal juros = totalAPagar.multiply(taxaJurosMensal)
                    .multiply(BigDecimal.valueOf(mesesAtraso));
            totalAPagar = totalAPagar.add(juros);
        }

        // 5. Verifica saldo
        if (user.getBalance().compareTo(totalAPagar) < 0) {
            throw new RuntimeException("Saldo insuficiente para pagar a fatura. Total devido: " + totalAPagar);
        }

        // 5.1 exige que a fatura já tenha sido fechada (closingDate <= hoje)
        if (invoice.getClosingDate().isAfter(hoje)) {
            throw new RuntimeException("Não é permitido pagar uma fatura antes do seu fechamento.");
        }

        // 6. Deduz saldo
        user.setBalance(user.getBalance().subtract(totalAPagar));
        userRepository.save(user);

        // 7. Marca fatura como paga
        invoice.setPaid(true);
        invoice.setTotalAmount(totalAPagar);
        creditInvoiceRepository.save(invoice);

        // 8. Atualiza cartão
        card.setCreditInvoice(card.getCreditInvoice().subtract(totalAPagar));
        creditCardRepository.save(card);

        // 9. Cria transação
        TransactionModel tx = new TransactionModel();
        tx.setUser(user);
        tx.setAmount(totalAPagar);
        tx.setDate(LocalDateTime.now());
        tx.setPaymentType("CREDIT_CARD");
        tx.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(tx);

        return invoice;
    }

    @Transactional
    public CreditInvoiceModel pagarFaturaPorId(UUID invoiceId, String email) {  // MUDEI: email direto, sem DTO
        // 1. Busca o usuário pelo e-mail
        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Busca a fatura específica pelo ID
        CreditInvoiceModel invoice = creditInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        // 3. Busca o cartão de crédito do usuário
        CreditCardModel card = creditCardRepository.findByUserOwnerId(user)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado"));

        // 4. Verifica se a fatura pertence ao usuário (segurança extra)
        if (!invoice.getCreditCardId().equals(card)) {  // Assuma getCreditCardId() no model
            throw new RuntimeException("Fatura não pertence a este usuário.");
        }

        // 5. Verifica se já está paga

        if (invoice.isPaid()) {
            throw new RuntimeException("Esta fatura já foi paga.");
        }

        // 5.1 exige que a fatura já tenha sido fechada (closingDate <= hoje)
        LocalDate hoje = LocalDate.now();
        if (invoice.getClosingDate().isAfter(hoje)) {
            throw new RuntimeException("Não é permitido pagar uma fatura antes do seu fechamento.");
        }

        // 6. Calcula juros se estiver vencida
        BigDecimal totalAPagar = invoice.getTotalAmount();
        if (hoje.isAfter(invoice.getDueDate())) {
            long mesesAtraso = ChronoUnit.MONTHS.between(
                    YearMonth.from(invoice.getDueDate()),
                    YearMonth.from(hoje)
            );
            BigDecimal taxaJurosMensal = new BigDecimal("0.01"); // 1% ao mês
            BigDecimal juros = totalAPagar.multiply(taxaJurosMensal)
                    .multiply(BigDecimal.valueOf(mesesAtraso));
            totalAPagar = totalAPagar.add(juros);
        }

        // 7. Verifica saldo
        if (user.getBalance().compareTo(totalAPagar) < 0) {
            throw new RuntimeException("Saldo insuficiente para pagar a fatura. Total devido: R$ " + totalAPagar);
        }

        // 8. Deduz saldo
        user.setBalance(user.getBalance().subtract(totalAPagar));
        userRepository.save(user);

        // 9. Marca fatura como paga
        invoice.setPaid(true);
        invoice.setTotalAmount(totalAPagar); // Atualiza com juros, se houver
        creditInvoiceRepository.save(invoice);

        // 10. Atualiza cartão (com check de null)
        if (card.getCreditInvoice() == null) {
            card.setCreditInvoice(BigDecimal.ZERO);
        }
        card.setCreditInvoice(card.getCreditInvoice().subtract(totalAPagar));
        creditCardRepository.save(card);

        // 11. Cria transação
        TransactionModel tx = new TransactionModel();
        tx.setUser(user);
        tx.setAmount(totalAPagar);
        tx.setDate(LocalDateTime.now());
        tx.setPaymentType("CREDIT_CARD");
        tx.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(tx);

        //TODO kafka email para usuario quando pagar fatura

        return invoice;
    }

    // Para pegar as faturas em aberto de um usuário (sem futuras zeradas)
    public List<CreditInvoiceModel> getOpenInvoicesByEmail(String email) {
        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        CreditCardModel card = creditCardRepository.findByUserOwnerId(user)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado"));

        LocalDate hoje = LocalDate.now();  // Data atual

        return card.getInvoices().stream()
                .filter(invoice -> !invoice.isPaid())  // Não paga
                .filter(invoice -> {
                    BigDecimal valor = invoice.getTotalAmount();  // Pega o valor
                    return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;  // > 0 e não null (pra BigDecimal)
                    // Alternativa se for double: return valor != null && valor > 0.0;
                })
                .sorted(Comparator.comparing(CreditInvoiceModel::getDueDate))  // MUDANÇA: Ordena por dueDate crescente (mais próxima de vencimento primeiro)
                .collect(Collectors.toList());
    }

    public List<ParcelModel> getInvoiceParcelsById(UUID invoiceId) {
        CreditInvoiceModel invoice = creditInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
        return parcelRepository.findByInvoiceOrderByParcelNumberAsc(invoice); // Assuma método no repo: List<ParcelModel> findByInvoiceOrderByParcelNumberAsc(CreditInvoiceModel invoice);
    }

    public CreditInvoiceModel getInvoiceById(UUID id) {
        return creditInvoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
    }
}
