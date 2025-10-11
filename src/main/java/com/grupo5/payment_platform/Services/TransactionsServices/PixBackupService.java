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
import com.grupo5.payment_platform.Repositories.PixPaymentDetailRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.grupo5.payment_platform.Services.UsersServices.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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

    public PixBackupService(TransactionRepository transactionRepository, UserService userService, PixPaymentDetailRepository pixPaymentDetailRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.pixPaymentDetailRepository = pixPaymentDetailRepository;
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

        return pixPaymentDetail;

    }


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
