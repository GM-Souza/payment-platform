package com.grupo5.payment_platform.Services.UsersServices;

import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserModel findById (UUID id) {
        return userRepository.findUserById(id);
    }
}
