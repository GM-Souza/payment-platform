package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.payments.PixPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PixPaymentDetailRepository extends JpaRepository<UUID, PixPaymentDetail> {
    static PixPaymentDetail findByQrCodeCopyPaste(String qrCodeCopyPaste);

}
