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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(SecurityFilter securityFilter, CustomUserDetailsService customUserDetailsService) {
        this.securityFilter = securityFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // usa a configuração de CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register-individual").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register-legalentity").permitAll()
                        .requestMatchers(HttpMethod.POST, "/individual").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/transactions/generateBoleto").permitAll()
                        .requestMatchers(HttpMethod.POST, "/transactions/deposito").permitAll()
                        .requestMatchers(HttpMethod.POST, "/transactions/saque").permitAll()
                        .requestMatchers(HttpMethod.POST, "transactions/pagarBoleto").permitAll()
                        .requestMatchers(HttpMethod.POST, "transactions/create-credit-card").permitAll()
                        .requestMatchers(HttpMethod.POST, "transactions/pagar-boleto-cartao").permitAll()
                        .requestMatchers(HttpMethod.GET, "transactions/get-card").permitAll()
                        .requestMatchers(HttpMethod.POST, "transactions/pagar-fatura-cartao").permitAll()
                        .requestMatchers(HttpMethod.POST, "transactions/pagar-pix-cartao").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/auth").hasRole("ADMIN")
                        .requestMatchers("/users/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean para criptografia de senha
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean para AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Configuração de CORS apenas para localhost:8081
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://agipay.vercel.app/")); // frontend local
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true); // importante para JWT ou cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
