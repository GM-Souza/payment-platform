package com.grupo5.payment_platform.Repositories;


import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Models.card.CreditCardModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCardModel, UUID> {

    Optional<CreditCardModel> findByUserOwnerId(UserModel userOwnerId);

}
