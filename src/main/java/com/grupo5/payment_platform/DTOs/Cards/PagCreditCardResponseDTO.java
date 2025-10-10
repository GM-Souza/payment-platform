package com.grupo5.payment_platform.DTOs.Cards;

public record PagCreditCardResponseDTO (String id, String creditCardId,
                                        String closingDate, String totalAmount,
                                        String paid)
{ }
