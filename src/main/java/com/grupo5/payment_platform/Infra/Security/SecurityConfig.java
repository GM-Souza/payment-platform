package com.grupo5.payment_platform.Infra.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

   private SecurityFilter securityFilter;
   private CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(SecurityFilter securityFilter, CustomUserDetailsService customUserDetailsService) {
        this.securityFilter = securityFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/register-individual").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/register-legalentity").permitAll()
                        .requestMatchers(HttpMethod.POST,"/individual").permitAll()
                        .requestMatchers(HttpMethod.POST,"/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST,"/transactions/generateBoleto").permitAll()
                        .requestMatchers(HttpMethod.POST,"/transactions/deposito").permitAll()
                        .requestMatchers(HttpMethod.POST,"/transactions/saque").permitAll()
                        .requestMatchers(HttpMethod.POST,"transactions/pagarBoleto").permitAll()
                        .requestMatchers(HttpMethod.POST,"transactions/create-credit-card").permitAll()
                        .requestMatchers(HttpMethod.POST,"transactions/get-card").permitAll()
                        .requestMatchers(HttpMethod.POST,"transactions//pagar-fatura-cartao").permitAll()
                        .requestMatchers(HttpMethod.DELETE,"/auth").hasRole("ADMIN")
                        .requestMatchers("/users/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    //Bean para criptografia de senha
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
