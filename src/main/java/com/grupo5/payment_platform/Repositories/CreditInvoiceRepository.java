package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditInvoiceRepository extends JpaRepository<CreditInvoiceModel, UUID> {

    List<CreditInvoiceModel> findByCreditCardIdOrderByClosingDateAsc(CreditCardModel creditCard);

    Optional<CreditInvoiceModel> findTopByCreditCardIdOrderByClosingDateDesc(CreditCardModel creditCardId);

    // Verifica se já existe uma fatura para determinado cartão e data de fechamento
    boolean existsByCreditCardIdAndClosingDate(CreditCardModel creditCardId, LocalDate closingDate);

    List<CreditInvoiceModel> findByCreditCardIdAndPaidFalse(CreditCardModel creditCardId);

    List<CreditInvoiceModel> findByCreditCardIdAndClosingDateAfterOrderByClosingDateAsc(
            CreditCardModel creditCardId, LocalDate date);
}
