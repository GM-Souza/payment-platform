package com.grupo5.payment_platform.Obsolete;

import java.math.BigDecimal;
import java.util.UUID;

public record UserRequestDTO (UUID id, String email, BigDecimal balance){
}
