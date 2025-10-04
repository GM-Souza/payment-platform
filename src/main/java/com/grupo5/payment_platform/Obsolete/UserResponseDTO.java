package com.grupo5.payment_platform.Obsolete;

import com.grupo5.payment_platform.Models.Users.UserModel;
import java.math.BigDecimal;

public record UserResponseDTO (String email, BigDecimal balance){

    public static UserResponseDTO fromUser (UserModel user) {
        return new UserResponseDTO(user.getEmail(), user.getBalance());
    }

}
