package com.grupo5.payment_platform.Configurations;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.acess-token}")
    private String acessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(acessToken);
    }
}
