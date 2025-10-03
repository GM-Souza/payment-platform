package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.payments.BoletoPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BoletoPaymentDetailRepository extends JpaRepository<BoletoPaymentDetail, UUID> {
    BoletoPaymentDetail findByBarcode(String barcode);
}
