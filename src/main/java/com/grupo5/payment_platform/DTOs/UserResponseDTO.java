package com.grupo5.payment_platform.DTOs;

import com.grupo5.payment_platform.Models.UserModel;
import java.math.BigDecimal;
import java.util.UUID;

public record UserResponseDTO (String email, BigDecimal balance){

    public static UserResponseDTO fromUser (UserModel user) {
        return new UserResponseDTO(user.getEmail(), user.getBalance());
    }

}
