package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.IndividualModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IndividualRepository extends JpaRepository<IndividualModel, UUID> {
}
