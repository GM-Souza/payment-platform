package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.card.CreditInvoiceModel;
import com.grupo5.payment_platform.Models.card.ParcelModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParcelRepository extends JpaRepository<ParcelModel, UUID> {

    List<ParcelModel> findByInvoiceOrderByParcelNumberAsc(CreditInvoiceModel invoice);
}
