package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.Exceptions.UserNotFoundException;
import com.grupo5.payment_platform.Models.UserModel;
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
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }
}
