package com.grupo5.payment_platform.Services.BoletoServices;

import com.grupo5.payment_platform.DTOs.BoletosDTOs.BoletoRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.UserLoginNotFoundException;
import com.grupo5.payment_platform.Models.Payments.BoletoModel;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.Users.IndividualModel;
import com.grupo5.payment_platform.Models.Users.LegalEntityModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Repositories.BoletoRepository;
import com.grupo5.payment_platform.Repositories.TransactionRepository;
import com.grupo5.payment_platform.Repositories.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class BoletoService {

    private final BoletoRepository boletoRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public BoletoService(BoletoRepository boletoRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.boletoRepository = boletoRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public byte[] generateBoletoPdf(BoletoRequestDTO dto) throws Exception {

        UserModel userModelSender = userRepository.findByEmail(dto.emailSender()).orElseThrow(() ->
                new UserLoginNotFoundException("Sender's email not found"));

        UserModel userModelReceiver = userRepository.findByEmail(dto.emailReceiver()).orElseThrow(() ->
                new UserLoginNotFoundException("Receiver's email not found"));

        Random random = new Random();
        String numeroDocumento = String.format("%06d", random.nextInt(1000000));
        String nossoNumero = String.format("01/%05d/1", random.nextInt(100000));

        LocalDate now = LocalDate.now();
        String dataEmissao = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String vencimentoStr = now.plusDays(30).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String bancoNome = "AGIBANK";
        String codigoCompensacao = "121";
        String linhaDigitavel = String.format("1219%s.01010 01010.01010 10000.0000%s 1 01010000000%s",
                String.format("%02d", random.nextInt(100)),
                numeroDocumento.charAt(0),
                numeroDocumento.substring(0, 3));
        String codigoBarras = linhaDigitavel.replaceAll("[^0-9]", "");

        String localPagamento = "PAGÁVEL PREFERENCIALMENTE NA REDE BANCÁRIA ATÉ O VENCIMENTO. APÓS O VENCIMENTO, PAGÁVEL EM QUALQUER BANCO OU LOTÉRICA.";
        String especieDoc = "DM";
        String aceite = "N";
        String usoBanco = "";
        String carteira = "01";
        String especieMoeda = "R$";
        String agenciaCodigo = "0001-0 / 01010-1";

        NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String valorSemSimbolo = formatoBR.format(dto.amount()).replace("R$", "").trim();
        String valorDocumento = valorSemSimbolo;

        String desconto = "0,00";
        String jurosMulta = "0,00";
        String outrosAcrescimos = "0,00";
        String valorCobrado = valorSemSimbolo;
        String deducoesAbatimentos = "0,00";

        String[] instrucoesLinhas = {
                "Após o vencimento, cobrar 2% de multa acrescida do valor principal.",
                "Juros de mora de 1% ao mês.",
                "Senhor Caixa: Cobrar do sacado o valor deste título acrescido de:",
                "2% no 1º dia de atraso",
                "1% ao mês após o 1º dia de atraso."
        };

        String nameReceiver;
        String documentReceiver;
        if (userModelReceiver instanceof IndividualModel individualReceiver) {
            nameReceiver = individualReceiver.getFullname();
            documentReceiver = individualReceiver.getCpf();
        } else if (userModelReceiver instanceof LegalEntityModel legalReceiver) {
            nameReceiver = legalReceiver.getLegalName();
            documentReceiver = legalReceiver.getCnpj();
        } else {
            throw new RuntimeException("Receiver not found!");
        }

        String nameSender;
        String documentSender;
        if (userModelSender instanceof IndividualModel individualSender) {
            nameSender = individualSender.getFullname();
            documentSender = individualSender.getCpf();
        } else if (userModelSender instanceof LegalEntityModel legalSender) {
            nameSender = legalSender.getLegalName();
            documentSender = legalSender.getCnpj();
        } else {
            throw new RuntimeException("Sender not found!");
        }

        byte[] barcodePng = gerarBarcodePng(codigoBarras, 400, 40);
        String pixContent = "00020101021226130014BR.GOV.BCB.PIX0136805a5b0a0c0f0g0h0111TESTE5204000053039865802BR5925Empresa Exemplo6009Sao Paulo62140510sacado@example.com6304A101";
        byte[] qrPng = gerarQRCodePng(pixContent, 120, 120);

        URL logoUrl = new URL("https://www.revistafatorbrasil.com.br/wp-content/uploads/2023/10/logo-agibanck.jpg");
        Image logoImage = Image.getInstance(logoUrl);
        logoImage.scaleToFit(180f, 60f);

        ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter.getInstance(document, pdfStream);
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font fontCourierBold = FontFactory.getFont(FontFactory.COURIER_BOLD, 11);
        Font fontCourier = FontFactory.getFont(FontFactory.COURIER, 10);

        // =========================
        // RECIBO DO SACADO (TOPO)
        // =========================
        PdfPTable reciboSacado = new PdfPTable(new float[]{0.2f, 0.6f, 0.2f});
        reciboSacado.setWidthPercentage(100);
        reciboSacado.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        reciboSacado.setSpacingAfter(10f);

        // Esquerda: Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        logoCell.addElement(logoImage);
        logoCell.setPadding(5f);

        // Centro: Título e linha digitável
        PdfPCell centroCell = new PdfPCell();
        centroCell.setBorder(Rectangle.NO_BORDER);
        centroCell.setPadding(5f);
        Paragraph tituloPar = new Paragraph(bancoNome + " - " + codigoCompensacao, fontTitulo);
        tituloPar.setAlignment(Element.ALIGN_CENTER);
        centroCell.addElement(tituloPar);
        Paragraph reciboPar = new Paragraph("RECIBO DO SACADO", fontBold);
        reciboPar.setAlignment(Element.ALIGN_CENTER);
        centroCell.addElement(reciboPar);
        Paragraph linhaPar = new Paragraph(linhaDigitavel, fontCourierBold);
        linhaPar.setAlignment(Element.ALIGN_CENTER);
        centroCell.addElement(linhaPar);

        // Direita: Nosso Número
        PdfPCell direitoFull = new PdfPCell();
        direitoFull.setBorder(Rectangle.NO_BORDER);
        direitoFull.setHorizontalAlignment(Element.ALIGN_RIGHT);
        direitoFull.setPadding(5f);
        direitoFull.addElement(new Phrase("Nosso Nº: " + nossoNumero, fontBold));

        // Adicionar células à tabela
        reciboSacado.addCell(logoCell);
        reciboSacado.addCell(centroCell);
        reciboSacado.addCell(direitoFull);

        // Adicionar bordas para formar uma caixa ao redor do recibo
        logoCell.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM);
        centroCell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        direitoFull.setBorder(Rectangle.RIGHT | Rectangle.TOP | Rectangle.BOTTOM);
        logoCell.setBorderColor(Color.BLACK);
        centroCell.setBorderColor(Color.BLACK);
        direitoFull.setBorderColor(Color.BLACK);

        // Linha superior para completar a borda
        PdfPTable topLineTable = new PdfPTable(1);
        topLineTable.setWidthPercentage(100);
        PdfPCell topLineCell = new PdfPCell(new Phrase(""));
        topLineCell.setBorder(Rectangle.TOP);
        topLineCell.setBorderColor(Color.BLACK);
        topLineTable.addCell(topLineCell);
        document.add(topLineTable);

        document.add(reciboSacado);

        // =========================
        // CORPO PRINCIPAL - CAMPOS SUPERIORES
        // =========================
        PdfPTable upperFields = new PdfPTable(new float[]{0.7f, 0.15f, 0.15f});
        upperFields.setWidthPercentage(100);

        // Local de Pagamento (esquerda)
        PdfPCell localFull = new PdfPCell();
        localFull.setBorder(Rectangle.BOTTOM);
        localFull.setPadding(3f);
        Paragraph localP = new Paragraph();
        localP.add(new Chunk("Local de Pagamento", fontSmall));
        localP.add(Chunk.NEWLINE);
        localP.add(new Chunk(localPagamento, fontNormal));
        localFull.addElement(localP);
        upperFields.addCell(localFull);

        // Vencimento
        PdfPCell vencFull = new PdfPCell();
        vencFull.setBorder(Rectangle.BOTTOM);
        vencFull.setPadding(3f);
        Paragraph vencP = new Paragraph();
        vencP.add(new Chunk("Vencimento", fontSmall));
        vencP.add(Chunk.NEWLINE);
        vencP.add(new Chunk(vencimentoStr, fontNormal));
        vencFull.addElement(vencP);
        upperFields.addCell(vencFull);

        // Número Documento
        PdfPCell docFull = new PdfPCell();
        docFull.setBorder(Rectangle.BOTTOM);
        docFull.setPadding(3f);
        Paragraph docP = new Paragraph();
        docP.add(new Chunk("Nr. Documento", fontSmall));
        docP.add(Chunk.NEWLINE);
        docP.add(new Chunk(numeroDocumento, fontNormal));
        docFull.addElement(docP);
        upperFields.addCell(docFull);

        document.add(upperFields);

        // =========================
        // CAMPOS MEIO - 4 COLUNAS
        // =========================
        PdfPTable meioFields = new PdfPTable(4);
        meioFields.setWidthPercentage(100);
        meioFields.setWidths(new float[]{0.25f, 0.25f, 0.25f, 0.25f});

        // Primeira linha
        meioFields.addCell(criarCelula("Espécie Doc.", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Aceite", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Data Process.", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Uso do Banco", fontSmall, true, Element.ALIGN_LEFT));

        // Valores
        meioFields.addCell(criarCelula(especieDoc, fontNormal, false, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula(aceite, fontNormal, false, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula(dataEmissao, fontNormal, false, Element.ALIGN_RIGHT));
        meioFields.addCell(criarCelula(usoBanco, fontNormal, false, Element.ALIGN_LEFT));

        // Segunda linha
        meioFields.addCell(criarCelula("Carteira", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Espécie Moeda", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Nosso Nº", fontSmall, true, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula("Agência/Cód Benef.", fontSmall, true, Element.ALIGN_LEFT));

        // Valores
        meioFields.addCell(criarCelula(carteira, fontNormal, false, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula(especieMoeda, fontNormal, false, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula(nossoNumero, fontNormal, false, Element.ALIGN_LEFT));
        meioFields.addCell(criarCelula(agenciaCodigo, fontNormal, false, Element.ALIGN_LEFT));

        document.add(meioFields);

        // =========================
        // SEÇÃO VALORES
        // =========================
        PdfPTable valoresTable = new PdfPTable(5);
        valoresTable.setWidthPercentage(100);
        valoresTable.setWidths(new float[]{0.3f, 0.175f, 0.175f, 0.175f, 0.175f});

        // Headers
        valoresTable.addCell(criarCelula("Valor Documento", fontSmall, true, Element.ALIGN_LEFT));
        valoresTable.addCell(criarCelula("(-) Desconto", fontSmall, true, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula("(+) Juros/Multa", fontSmall, true, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula("(+) Outros Acr.", fontSmall, true, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula("= Valor Cobrado", fontSmall, true, Element.ALIGN_RIGHT));

        // Valores
        valoresTable.addCell(criarCelula(valorDocumento, fontCourier, false, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula(desconto, fontCourier, false, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula(jurosMulta, fontCourier, false, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula(outrosAcrescimos, fontCourier, false, Element.ALIGN_RIGHT));
        valoresTable.addCell(criarCelula(valorCobrado, fontCourierBold, false, Element.ALIGN_RIGHT));

        // Linha adicional para deduções
        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(criarCelula("(-) Outras Ded./Abat.", fontSmall, true, Element.ALIGN_RIGHT));
        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(new PdfPCell(new Phrase("")));

        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(criarCelula(deducoesAbatimentos, fontCourier, false, Element.ALIGN_RIGHT));
        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(new PdfPCell(new Phrase("")));
        valoresTable.addCell(new PdfPCell(new Phrase("")));

        document.add(valoresTable);
        document.add(Chunk.NEWLINE);

        // =========================
        // INSTRUÇÕES E BENEFICIÁRIO
        // =========================
        PdfPTable instrBenefTable = new PdfPTable(2);
        instrBenefTable.setWidthPercentage(100);
        instrBenefTable.setWidths(new float[]{0.6f, 0.4f});

        // Esquerda: Instruções
        PdfPCell instrCell = new PdfPCell();
        instrCell.setBorder(Rectangle.BOX);
        instrCell.setPadding(5f);
        Paragraph instrTitle = new Paragraph("Instruções do Beneficiário", fontBold);
        instrTitle.setAlignment(Element.ALIGN_CENTER);
        instrCell.addElement(instrTitle);
        for (String linha : instrucoesLinhas) {
            instrCell.addElement(new Paragraph(linha, fontNormal));
        }
        instrBenefTable.addCell(instrCell);

        // Direita: Beneficiário
        PdfPCell benefCell = new PdfPCell();
        benefCell.setBorder(Rectangle.BOX);
        benefCell.setPadding(5f);
        Paragraph benefTitle = new Paragraph("Beneficiário", fontBold);
        benefTitle.setAlignment(Element.ALIGN_CENTER);
        benefCell.addElement(benefTitle);
        benefCell.addElement(new Paragraph(nameReceiver, fontNormal));
        benefCell.addElement(new Paragraph("CNPJ: " + documentReceiver, fontSmall));
        //por enquanto, nao teremos endereços..
        //benefCell.addElement(new Paragraph(enderecoBeneficiario, fontSmall));
        instrBenefTable.addCell(benefCell);

        document.add(instrBenefTable);
        document.add(Chunk.NEWLINE);

        // =========================
        // SACADO
        // =========================
        PdfPTable sacadoTable = new PdfPTable(1);
        sacadoTable.setWidthPercentage(100);
        PdfPCell sacadoCell = new PdfPCell();
        sacadoCell.setBorder(Rectangle.BOX);
        sacadoCell.setPadding(5f);
        Paragraph sacadoTitle = new Paragraph("Sacado", fontBold);
        sacadoTitle.setAlignment(Element.ALIGN_CENTER);
        sacadoCell.addElement(sacadoTitle);
        sacadoCell.addElement(new Paragraph(nameSender, fontNormal));
        sacadoCell.addElement(new Paragraph("CPF: " + documentSender, fontSmall));
        //por enquanto, nao teremos endereços..
        //sacadoCell.addElement(new Paragraph(enderecoSacado, fontSmall));
        sacadoTable.addCell(sacadoCell);
        document.add(sacadoTable);
        document.add(Chunk.NEWLINE);

        // =========================
        // CÓDIGO DE BARRAS E QR
        // =========================
        PdfPTable bottomTable = new PdfPTable(1);
        bottomTable.setWidthPercentage(100);

        // Barcode
        PdfPCell barcodeCell = new PdfPCell();
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        barcodeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image barcodeImg = Image.getInstance(barcodePng);
        barcodeImg.scaleToFit(400, 40);
        barcodeCell.addElement(barcodeImg);

        // Adicionar linha digitável antes de adicionar a célula à tabela
        Paragraph linhaDigitavelBottom = new Paragraph(linhaDigitavel, fontCourierBold);
        linhaDigitavelBottom.setAlignment(Element.ALIGN_CENTER);
        barcodeCell.addElement(linhaDigitavelBottom);

        bottomTable.addCell(barcodeCell);

        // QR Code abaixo
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setPaddingTop(10f);
        Image qrImg = Image.getInstance(qrPng);
        qrImg.scaleToFit(120, 120);
        qrCell.addElement(qrImg);
        bottomTable.addCell(qrCell);

        document.add(bottomTable);

        document.close();


        // Criar a transação e o detalhe
        BoletoModel boletoTx = new BoletoModel();
        boletoTx.setReceiver(userModelReceiver);
        boletoTx.setSender(userModelSender);
        boletoTx.setAmount(dto.amount());
        boletoTx.setDate(LocalDateTime.now());
        boletoTx.setStatus(TransactionStatus.PENDING);
        boletoTx.setPaymentType("BOLETO");
        boletoTx.setDueDate(LocalDate.parse(vencimentoStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        boletoTx.setUser(userModelReceiver);

         // Criar e associar o detalhe
        BoletoPaymentDetail detail = new BoletoPaymentDetail();
        detail.setBoletoCode(linhaDigitavel);
        detail.setDocumentCode(numeroDocumento);
        detail.setOurNumber(nossoNumero);
        detail.setBoletoTransaction(boletoTx); // Configura a relação
        boletoTx.setBoletoPaymentDetail(detail); // Configura a relação bidirecional

        // Salvar a transação (o cascade persiste o BoletoPaymentDetail)
        transactionRepository.save(boletoTx);

        // Retornar o PDF
        return pdfStream.toByteArray();
    }

    private PdfPCell criarCelula(String texto, Font fonte, boolean cabecalho, int alignment) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setHorizontalAlignment(alignment);
        c.setPadding(4f);
        if (cabecalho) {
            c.setBackgroundColor(new Color(240, 240, 240));
        }
        return c;
    }

    private byte[] gerarBarcodePng(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.ITF, width, height, hints);
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngStream);
        return pngStream.toByteArray();
    }

    private byte[] gerarQRCodePng(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngStream);
        return pngStream.toByteArray();
    }
}
