package com.grupo5.payment_platform.Repositories;

import com.grupo5.payment_platform.Models.Users.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserModel, UUID> {
    UserModel findUserById(UUID id);
}
