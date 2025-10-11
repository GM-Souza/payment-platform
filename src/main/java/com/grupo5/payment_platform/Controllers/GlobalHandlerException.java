package com.grupo5.payment_platform.Controllers;

import com.grupo5.payment_platform.DTOs.ErrorResponseDTO;
import com.grupo5.payment_platform.Exceptions.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalHandlerException {

    //Tratamento de exceção para quando o ID do usuário não for encontrado no banco de dados
    @ExceptionHandler(IndividualNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO IndividualNotFoundException(IndividualNotFoundException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                ex.getMessage(),
                "Não foi possível localizar o Email fornecido para a conta da Entidade. Verifique o valor e tente novamente.",
                LocalDateTime.now()
        );
        return errorResponseDTO;
    }

    @ExceptionHandler(LegalEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO LegalEntityNotFoundException(LegalEntityNotFoundException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                ex.getMessage(),
                "Não foi possível localizar o Email fornecido para a conta da Entidade. Verifique o valor e tente novamente.",
                LocalDateTime.now()
        );
        return errorResponseDTO;
    }

    //Tratamento de exceção para quando o saldo for insuficiente
    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleInsufficientBalance(InsufficientBalanceException ex) {
        ex.getStackTrace();
        return new ErrorResponseDTO(
                ex.getMessage(),
                "A conta do remetente não tem fundos suficientes para concluir a transação.",
                LocalDateTime.now()
        );
    }

    //Tratamento de exceção para quando o valor da transação for inválido
    @ExceptionHandler(InvalidTransactionAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleInvalidAmount(InvalidTransactionAmountException ex) {
        return new ErrorResponseDTO(
                ex.getMessage(),
                "O valor da transação informado é inválido. Verifique e tente novamente.",
                LocalDateTime.now()
        );
    }

    //Tratamento de exceção para quando o ID do usuário não for encontrado no banco de dados
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleUserNotFound(UserNotFoundException ex) {
        return new ErrorResponseDTO(
                ex.getMessage(),
                "O ID de usuário fornecido não foi encontrado. Verifique e tente novamente.",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(CodeBoletoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO codeBoletoNotFoundException(CodeBoletoNotFoundException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                ex.getMessage(),
                "O código do boleto fornecido não foi encontrado. Verifique e tente novamente.",
                LocalDateTime.now()
        );
        return errorResponseDTO;
    }
}
