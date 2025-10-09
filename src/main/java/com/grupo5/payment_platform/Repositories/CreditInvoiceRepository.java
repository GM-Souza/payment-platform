package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.Card.CreditCardModel;
import com.grupo5.payment_platform.Models.Card.CreditInvoiceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditInvoiceRepository extends JpaRepository<CreditInvoiceModel, UUID> {

    List<CreditInvoiceModel> findByCreditCardIdOrderByClosingDateAsc(CreditCardModel creditCard);

    // Retorna a última fatura (com data de fechamento mais recente) de um cartão
    Optional<CreditInvoiceModel> findTopByCreditCardIdOrderByClosingDateDesc(CreditCardModel creditCardId);

    // Verifica se já existe uma fatura para determinado cartão e data de fechamento
    boolean existsByCreditCardIdAndClosingDate(CreditCardModel creditCardId, LocalDate closingDate);

    List<CreditInvoiceModel> findByCreditCardIdAndClosingDateAfterOrderByClosingDateAsc(
            CreditCardModel creditCardId, LocalDate date);
}