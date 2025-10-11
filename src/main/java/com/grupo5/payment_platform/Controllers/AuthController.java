package com.grupo5.payment_platform.Controllers;

import com.grupo5.payment_platform.DTOs.AuthDTOs.IndividualRequestDTO;
import com.grupo5.payment_platform.DTOs.AuthDTOs.LegalEntityRequestDTO;
import com.grupo5.payment_platform.DTOs.AuthDTOs.LoginRequestDTO;
import com.grupo5.payment_platform.DTOs.AuthDTOs.LoginResponseDTO;
import com.grupo5.payment_platform.Enums.EmailSubject;
import com.grupo5.payment_platform.Exceptions.UserLoginNotFoundException;
import com.grupo5.payment_platform.Infra.Kafka.TransactionNotificationDTO;
import com.grupo5.payment_platform.Infra.Security.TokenService;
import com.grupo5.payment_platform.Models.Users.IndividualModel;
import com.grupo5.payment_platform.Models.Users.LegalEntityModel;
import com.grupo5.payment_platform.Models.Users.UserModel;
import com.grupo5.payment_platform.Repositories.IndividualRepository;
import com.grupo5.payment_platform.Repositories.LegalEntityRepository;
import com.grupo5.payment_platform.Repositories.UserRepository;
import com.grupo5.payment_platform.Services.TransactionKafkaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final IndividualRepository individualRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final TransactionKafkaService transactionKafkaService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, IndividualRepository individualRepository, LegalEntityRepository legalEntityRepository, TransactionKafkaService transactionKafkaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.individualRepository = individualRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.transactionKafkaService = transactionKafkaService;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequestDTO loginRequestDTO) {
        UserModel user = userRepository.findByEmail(loginRequestDTO.email()).orElseThrow(() -> new UserLoginNotFoundException("As credencias não batem com nenhum usuario"));
        if(passwordEncoder.matches(loginRequestDTO.password(), user.getPassword())) {
            String token = this.tokenService.generateToken(user);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        }
        return  ResponseEntity.badRequest().build();
    }

    //Metodo para criar um usuario(Pessoa Fisica)
    @PostMapping("/register-individual")
    public ResponseEntity registerIndividual(@RequestBody IndividualRequestDTO individualRequestDTO) {
        Optional<UserModel> user = userRepository.findByEmail(individualRequestDTO.email());
        if (user.isEmpty()){
            IndividualModel individual = new IndividualModel();
            individual.setFullname(individualRequestDTO.fullname());
            individual.setCpf(individualRequestDTO.cpf());
            individual.setEmail(individualRequestDTO.email());
            individual.setPassword(passwordEncoder.encode(individualRequestDTO.password()));
            individual.setBalance(individualRequestDTO.balance());
            TransactionNotificationDTO notify = new TransactionNotificationDTO(individual.getFullname(),individual.getEmail(), EmailSubject.WELCOME);
            transactionKafkaService.sendWelcomeNotification(notify);
            individualRepository.save(individual);

            String token = this.tokenService.generateToken(individual);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/register-legalentity")
    public ResponseEntity registerLegalEntity(@RequestBody LegalEntityRequestDTO legalEntityRequestDTO) {
        Optional <UserModel> user = userRepository.findByEmail(legalEntityRequestDTO.email());
        if (user.isEmpty()){
            LegalEntityModel legalEntity = new LegalEntityModel();
            legalEntity.setLegalName(legalEntityRequestDTO.legalName());
            legalEntity.setCnpj(legalEntityRequestDTO.cnpj());
            legalEntity.setEmail(legalEntityRequestDTO.email());
            legalEntity.setPassword(passwordEncoder.encode(legalEntityRequestDTO.password()));
            legalEntity.setBalance(legalEntityRequestDTO.balance());
            TransactionNotificationDTO notify = new TransactionNotificationDTO(legalEntity.getLegalName(),legalEntity.getEmail(), EmailSubject.WELCOME);
            transactionKafkaService.sendWelcomeNotification(notify);
            legalEntityRepository.save(legalEntity);

            String token = this.tokenService.generateToken(legalEntity);
            return ResponseEntity.ok(new LoginResponseDTO(token));
        }
        return ResponseEntity.badRequest().build();
    }
}
