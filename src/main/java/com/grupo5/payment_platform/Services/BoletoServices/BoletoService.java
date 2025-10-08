package com.grupo5.payment_platform.Services.BoletoServices;

import com.grupo5.payment_platform.DTOs.BoletosDTOs.BoletoRequestDTO;
import com.grupo5.payment_platform.Enums.TransactionStatus;
import com.grupo5.payment_platform.Exceptions.UserLoginNotFoundException;
import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import com.grupo5.payment_platform.Models.Payments.TransactionModel;
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

    // Construtor corrigido: REMOVIDO o parâmetro BoletoPaymentDetail (não é injetável!)
    public BoletoService(BoletoRepository boletoRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.boletoRepository = boletoRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    //a intenção com este codigo é receber o email do front e verificar se o Receiver existe.
    //também iremos pedir o email do Sender(ele terá que ser um cliente da plataforma).
    public byte[] generateBoletoPdf(BoletoRequestDTO dto) throws Exception {

        UserModel userModelReceiver = userRepository.findByEmail(dto.emailSender()).orElseThrow(()->
                new UserLoginNotFoundException("Sender's email not found"));

        UserModel userModelSender = userRepository.findByEmail(dto.emailReceiver()).orElseThrow(()->
                new UserLoginNotFoundException("Receiver's email not found"));


        // Gerar dados aleatórios e dinâmicos
        Random random = new Random();
        String numeroDocumento = String.format("%06d", random.nextInt(1000000)); // Número aleatório de 6 dígitos
        String nossoNumero = String.format("01/%05d/1", random.nextInt(100000)); // Nosso número aleatório no formato exemplo

        LocalDate now = LocalDate.now();
        String dataEmissao = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String vencimento = now.plusDays(30).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // --- Dados de exemplo atualizados para Agibank ---
        String bancoNome = "AGIBANK";
        String codigoCompensacao = "121";
        // Linha digitável fictícia atualizada com partes aleatórias
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

        //convertendo tipo bigdecimal em String para jogar no pdf o valor como string...
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

       //criando um cast, para poder pegar o nome do Receiver
        String nameReceiver = null;
        String documentreceiver = null;
        if (userModelReceiver instanceof IndividualModel) {
            IndividualModel individualReceiver = (IndividualModel) userModelReceiver;
            nameReceiver = individualReceiver.getFullname();
            documentreceiver = individualReceiver.getCpf();
        } else if (userModelReceiver instanceof LegalEntityModel){
            LegalEntityModel legalReceiver= (LegalEntityModel) userModelReceiver;
            nameReceiver = legalReceiver.getLegalName();
            documentreceiver = legalReceiver.getCnpj();
        }else{
            throw new RuntimeException("Receiver not found!");
        }


        String beneficiario = nameReceiver;
        String cnpjBeneficiario = documentreceiver;

        //Por enquanto sem endereço
        //String enderecoBeneficiario = "Rua Teste, 987 - Centro - Aracaju/SE - CEP: 49000-000";

        //criando um cast, para poder pegar o nome do Sender
        String nameSender = null;
        String documentSender = null;
        if (userModelSender instanceof IndividualModel) {
            IndividualModel individualSender = (IndividualModel) userModelSender;
            nameSender = individualSender.getFullname();
            documentSender = individualSender.getCpf();
        } else if (userModelSender instanceof LegalEntityModel){
            LegalEntityModel legalSender = (LegalEntityModel) userModelSender;
            nameSender = legalSender.getLegalName();
            documentSender = legalSender.getCnpj();
        }else{
            throw new RuntimeException("Sender not found!");
        }

        String sacado = nameSender;
        String cpfSacado = documentSender;

        //Por enquanto sem endereço
        //String enderecoSacado = "Rua Exemplo, 123 - Bairro Centro - Maringá/PR - CEP: 87098-765";

        // --- Gerar código de barras (ITF para boleto brasileiro) ---
        byte[] barcodePng = gerarBarcodePng(codigoBarras, 400, 40);

        // --- Gerar QR Code PIX (exemplo fictício) ---
        String pixContent = "00020101021226130014BR.GOV.BCB.PIX0136805a5b0a0c0f0g0h0111TESTE5204000053039865802BR5925Empresa Exemplo6009Sao Paulo62140510sacado@example.com6304A101";
        byte[] qrPng = gerarQRCodePng(pixContent, 120, 120);

        // --- Carregar logo do Agibank ---
        URL logoUrl = new URL("https://www.revistafatorbrasil.com.br/wp-content/uploads/2023/10/logo-agibanck.jpg");
        Image logoImage = Image.getInstance(logoUrl);
        logoImage.scaleToFit(180f, 60f);

        // --- Criar PDF ---
        ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter.getInstance(document, pdfStream);
        document.open();

        // --- Fontes ---
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
        vencP.add(new Chunk(vencimento, fontNormal));
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
        benefCell.addElement(new Paragraph(beneficiario, fontNormal));
        benefCell.addElement(new Paragraph("CNPJ: " + cnpjBeneficiario, fontSmall));
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
        sacadoCell.addElement(new Paragraph(sacado, fontNormal));
        sacadoCell.addElement(new Paragraph("CPF: " + cpfSacado, fontSmall));
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

        // Salvar na tabela de transação as informações
        TransactionModel transaction = new TransactionModel();
        transaction.setSender(userModelSender);
        transaction.setReceiver(userModelReceiver);
        transaction.setAmount(dto.amount());
        transaction.setCreateDate(LocalDateTime.now());
        transaction.setFinalDate(null);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        //salvar na tabela de Boleto as informações
        BoletoPaymentDetail boletoPaymentDetail = new BoletoPaymentDetail();

        boletoPaymentDetail.setBoletoCode(linhaDigitavel);
        boletoPaymentDetail.setDocumentCode(numeroDocumento);

        boletoPaymentDetail.setOurNumber(nossoNumero);

        //transformando o vencimento que está em String para LocalDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate venc = LocalDate.parse(vencimento, formatter);
        boletoPaymentDetail.setDueDate(venc);
        boletoPaymentDetail.setTransaction(transaction);

        // Salva o BoletoPaymentDetails no banco de dados
        boletoRepository.save(boletoPaymentDetail);

        // Atualiza a transação com o boletoPaymentDetail
        transaction.setPaymentDetail(boletoPaymentDetail);
        transactionRepository.save(transaction);

        return pdfStream.toByteArray();
    }

    // Método para criar células aprimorado com alinhamento
    private PdfPCell criarCelula(String texto, Font fonte, boolean cabecalho, int alignment) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setHorizontalAlignment(alignment);
        c.setPadding(4f);
        if (cabecalho) {
            c.setBackgroundColor(new Color(240, 240, 240));
        }
        return c;
    }

    // Gerar código de barras ITF
    private byte[] gerarBarcodePng(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.ITF, width, height, hints);
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngStream);
        return pngStream.toByteArray();
    }

    // Gerar QR Code
    private byte[] gerarQRCodePng(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngStream);


        return pngStream.toByteArray();
    }
}




