package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.UserResponseDTO;
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

    public UserModel findById (UUID id) throws Exception {
        return userRepository.findById(id).orElseThrow(() -> new Exception("Usuário não encontrado"));
    }
}
