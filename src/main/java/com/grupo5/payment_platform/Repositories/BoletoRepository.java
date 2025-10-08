package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.Payments.BoletoPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoletoRepository extends JpaRepository<BoletoPaymentDetail, Long> {

    Optional<BoletoPaymentDetail> findByBoletoCode(String boletoCode);
}
