package com.grupo5.payment_platform.Exceptions;

import com.grupo5.payment_platform.DTOs.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalHandlerException {


    @ExceptionHandler(IndividualNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO IndividualNotFoundException(IndividualNotFoundException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                ex.getMessage(),
                "he provided ID could not be located for the Individual account. Please verify the value and try again.",
                LocalDateTime.now()
        );
        return errorResponseDTO;
    }

    @ExceptionHandler(LegalEntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO LegalEntityNotFoundException(LegalEntityNotFoundException ex){
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                ex.getMessage(),
                "he provided ID could not be located for the Entity account. Please verify the value and try again.",
                LocalDateTime.now()
        );
        return errorResponseDTO;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleDataIntegrity(DataIntegrityViolationException ex) {
        String detailMessage = "Data is invalid or already exists.";

        //Se o cpf ou CNPJ estiver duplicado, o Banco de dados devolverá uma mensagem que contem a palvra CPF ou CNPJ
        //criamos um método para ler esta mensagem do BD e alterar nosso erro se perceber que o erro se trata de CPF ou CNPJ
        String causeMessage = ex.getMostSpecificCause().getMessage().toLowerCase();
        if (causeMessage.contains("cpf")) {
            detailMessage = "The provided CPF already exists.";
        } else if (causeMessage.contains("cnpj")) {
            detailMessage = "The provided CNPJ already exists.";
        }

        return new ErrorResponseDTO(
                "Conflict",
                detailMessage,
                LocalDateTime.now()
        );
    }

}
