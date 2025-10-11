package com.grupo5.payment_platform.Services.TransactionsServices;

import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixReceiverRequestDTO;
import com.grupo5.payment_platform.DTOs.PixDTOs.PixSenderRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.InsufficientBalanceException;
import com.grupo5.payment_platform.Exceptions.InvalidTransactionAmountException;
import com.grupo5.payment_platform.Exceptions.PixQrCodeNotFoundException;
import com.grupo5.payment_platform.Exceptions.UserNotFoundException;
import com.grupo5.payment_platform.Models.Payments.PixModel;
import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Models.card.ParcelModel;
import com.grupo5.payment_platform.Repositories.*;
import com.grupo5.payment_platform.Services.UsersServices.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PixBackupService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final PixPaymentDetailRepository pixPaymentDetailRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditInvoiceRepository creditInvoiceRepository;
    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;

    public PixBackupService(TransactionRepository transactionRepository, UserService userService,
                            PixPaymentDetailRepository pixPaymentDetailRepository, CreditCardRepository creditCardRepository,
                            CreditInvoiceRepository creditInvoiceRepository, ParcelRepository parcelRepository,
                            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditInvoiceRepository = creditInvoiceRepository;
        this.parcelRepository = parcelRepository;
        this.userRepository = userRepository;
    }

    public PixPaymentDetail criarCobrancaPix(PixReceiverRequestDTO dto) throws Exception{

       Optional<UserModel> receiver = userService.findByEmail(dto.receiverEmail());
         if(receiver.isEmpty()){
              throw new UserNotFoundException("Usuário não encontrado");
         }
            if(dto.amount().compareTo(BigDecimal.ZERO) <= 0){
                throw new InvalidTransactionAmountException("Valor inválido para a transação");
            }
            PixModel pixTransaction = new PixModel();
            pixTransaction.setAmount(dto.amount());
            pixTransaction.setReceiver(receiver.get());
            pixTransaction.setPaymentType("PIX");
            pixTransaction.setUser(null);
            pixTransaction.setStatus(TransactionStatus.PENDING);
            pixTransaction.setDate(LocalDateTime.now());
        transactionRepository.save(pixTransaction);

        String copyPaste = UUID.randomUUID() + dto.amount().toPlainString();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitmatrix;
        try{
            bitmatrix = qrCodeWriter.encode(copyPaste, com.google.zxing.BarcodeFormat.QR_CODE, 350, 350);
        }catch (WriterException e){
            throw new RuntimeException("Error to generate QR Code", e);
        }
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitmatrix, new MatrixToImageConfig());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        String dataUrl = "data:image/png;base64," + base64Image;
        PixPaymentDetail pixPaymentDetail = new PixPaymentDetail();

        pixPaymentDetail.setPixTransaction(pixTransaction);
        pixPaymentDetail.setQrCodeBase64(dataUrl);
        pixPaymentDetail.setQrCodeCopyPaste(copyPaste);
        pixPaymentDetailRepository.save(pixPaymentDetail);
        //email kafka

        return pixPaymentDetail;

    }

    //pagamento de backup mas provavelmente nao sera usado
    @Transactional
    public PixModel pagarViaPixCopyPaste(PixSenderRequestDTO dto){

        // Busca o detalhe da cobrança pelo código Pix
    
   @Transactional
    public PixModel pagarPixViaCreditCard(PixSenderRequestDTO dto, int parcelas) {
        // Busca o detalhe da cobrança Pix pelo código copy-paste
        PixPaymentDetail pixDetail = pixPaymentDetailRepository.findByQrCodeCopyPaste(dto.qrCodeCopyPaste());

        if (pixDetail == null) {
            throw new PixQrCodeNotFoundException("Cobrança Pix não encontrada.");
        }

        PixModel transaction = pixDetail.getPixTransaction();

        // Verifica se a transação está pendente
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new InvalidTransactionAmountException("Essa cobrança já foi paga ou cancelada.");
        }

        // Busca o pagador
        UserModel sender = userService.findByEmail(dto.senderEmail())
                .orElseThrow(() -> new UserNotFoundException("Pagador não encontrado."));

        UserModel receiver = transaction.getReceiver();
        BigDecimal amount = transaction.getAmount();

        // Busca o cartão do pagador
        CreditCardModel card = creditCardRepository.findByUserOwnerId(sender)
                .orElseThrow(() -> new RuntimeException("Cartão de crédito não encontrado para o usuário."));

        BigDecimal limiteMensal = card.getCreditLimit();

        // Soma o total já faturado no mês atual (faturas abertas e não pagas)
        LocalDate now = LocalDate.now();
        BigDecimal totalMesAtual = card.getInvoices().stream()
                .filter(inv -> !inv.isPaid())
                .filter(inv -> inv.getClosingDate().getMonth().equals(now.getMonth()) &&
                        inv.getClosingDate().getYear() == now.getYear())
                .map(CreditInvoiceModel::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal limiteDisponivel = limiteMensal.subtract(totalMesAtual);

        if (amount.compareTo(limiteDisponivel) > 0) {
            throw new RuntimeException("Limite de crédito insuficiente para realizar esta transação.");
        }

        // Busca faturas futuras
        List<CreditInvoiceModel> futureInvoices = creditInvoiceRepository
                .findByCreditCardIdAndClosingDateAfterOrderByClosingDateAsc(card, now);

        if (futureInvoices.size() < parcelas) {
            throw new RuntimeException("Não há faturas futuras suficientes para distribuir as parcelas.");
        }

        // Valor por parcela
        BigDecimal valorParcela = amount.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP.HALF_UP);

        // Cria as parcelas
        for (int i = 0; i < parcelas; i++) {
            CreditInvoiceModel invoice = futureInvoices.get(i);

            ParcelModel parcel = new ParcelModel();
            parcel.setInvoice(invoice);
            parcel.setOriginalTransaction(transaction); // linka a transação Pix
            parcel.setParcelNumber(i + 1);
            parcel.setTotalParcels(parcelas);
            parcel.setAmount(valorParcela);
            parcel.setDescription("Parcela " + (i + 1) + " de " + parcelas + " do pagamento Pix.");

            // Relacionamento bidirecional
            invoice.getParcels().add(parcel);
            parcelRepository.save(parcel);

            invoice.setTotalAmount(invoice.getTotalAmount().add(valorParcela));
            creditInvoiceRepository.save(invoice);
        }

        // Recalcula o total das faturas afetadas
        futureInvoices.forEach(invoice -> {
            invoice.recalculateTotalAmount();
            creditInvoiceRepository.save(invoice);
        });

        // Atualiza transação Pix
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setPaymentType("PIX_CREDIT_CARD");
        transaction.setFinalDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Credita o valor ao recebedor
        receiver.setBalance(receiver.getBalance().add(amount));
        userRepository.save(receiver);

        return transaction;
    }

    @Scheduled(fixedRate = 6000000)
    public void cancelarPixPendentes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(1);

        List<TransactionModel> pendentes = transactionRepository.findByStatusAndDateBefore(TransactionStatus.PENDING, limite);

        for (TransactionModel transacao : pendentes) {
            transacao.setStatus(TransactionStatus.CANCELLED);
            transactionRepository.save(transacao);
        }
    }
}
