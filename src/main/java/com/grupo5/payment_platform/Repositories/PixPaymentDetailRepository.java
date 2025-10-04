package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.Payments.PixPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PixPaymentDetailRepository extends JpaRepository<PixPaymentDetail,UUID> {
     PixPaymentDetail findByQrCodeCopyPaste(String qrCodeCopyPaste);
}
