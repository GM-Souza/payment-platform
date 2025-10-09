package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.card.CreditCardModel;
import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditInvoiceRepository extends JpaRepository<CreditInvoiceModel, UUID> {

    List<CreditInvoiceModel> findByCreditCardIdOrderByClosingDateAsc(CreditCardModel creditCard);
}
