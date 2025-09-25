package com.grupo5.payment_platform.DTOs;

import java.time.LocalDateTime;

public record ErrorResponseDTO(String error,String detail,LocalDateTime time ) {

}
