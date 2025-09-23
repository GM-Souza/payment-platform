package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.LegalEntityModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LegalEntityRepository extends JpaRepository<LegalEntityModel, UUID> {
}
