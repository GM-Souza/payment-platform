package com.grupo5.payment_platform.DTOs.Cards;

public record CreditCardResponseDTO(String creditNumber,String cvv,String expiration,String creditLimit) {
}
