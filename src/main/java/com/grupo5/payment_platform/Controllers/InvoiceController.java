package com.grupo5.payment_platform.Controllers;


import com.grupo5.payment_platform.DTOs.Cards.PagCreditCardRequestDTO;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Models.card.ParcelModel;
import com.grupo5.payment_platform.Services.TransactionsServices.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    // No metodo GET de detalhes, certifique-se de passar "invoice":
    @GetMapping("/invoice/{id}/details")
    public String getInvoiceDetails(@PathVariable UUID id, Model model) {
        CreditInvoiceModel invoice = transactionService.getInvoiceById(id);  // Busca a fatura completa
        List<ParcelModel> parcels = transactionService.getInvoiceParcelsById(id);

        // Busca email via chain (como antes)
        String email = invoice.getCreditCardId().getUserOwnerId().getEmail();  // Assuma getters

        model.addAttribute("invoice", invoice);  // NOVO: Pra usar totalAmount no botão
        model.addAttribute("parcels", parcels);
        model.addAttribute("email", email);
        model.addAttribute("dataGeracao", LocalDate.now());
        model.addAttribute("invoiceId", id);

        return "invoice-details";
    }

    // O POST corrigido:
    @PostMapping("/invoice/{id}/pay")
    public String pagarFatura(@PathVariable UUID id, @RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            CreditInvoiceModel paidInvoice = transactionService.pagarFaturaPorId(id, email);  // Chama com email direto
            redirectAttributes.addFlashAttribute("successMessage", "Fatura paga com sucesso! Total: R$ " + paidInvoice.getTotalAmount());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/credit-card/extrato-faturas?email=" + email;
    }
}
