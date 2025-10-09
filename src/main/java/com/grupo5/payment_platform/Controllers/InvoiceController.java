package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Services.TransactionsServices.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/credit-card")
public class InvoiceController {

    private final TransactionService transactionService;

    public InvoiceController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/extrato-faturas")
    public String extratoFaturas(@RequestParam String email, Model model) {
        List<CreditInvoiceModel> openInvoices = transactionService.getOpenInvoicesByEmail(email);
        model.addAttribute("openInvoices", openInvoices);
        model.addAttribute("email", email);
        model.addAttribute("dataGeracao", LocalDate.now());

        // Calcula total opcional para o extratos
        BigDecimal totalAberto = openInvoices.stream()
                .map(CreditInvoiceModel::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalAberto", totalAberto);

        return "extrato-faturas"; // Nome do template Thymeleaf
    }
}
