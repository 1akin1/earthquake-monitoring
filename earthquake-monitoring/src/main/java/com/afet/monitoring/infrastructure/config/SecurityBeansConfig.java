package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

/**
 * Wires the security primitives that are pure beans: the JDK-only {@link JwtService}
 * (secret + token lifetime come from config) and a BCrypt {@link PasswordEncoder} for the
 * demo user store. The secret should be supplied via the {@code JWT_SECRET} env var in any
 * real deployment; the default is for local development only.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    JwtService jwtService(@Value("${app.security.jwt.secret}") String secret,
                          @Value("${app.security.jwt.ttl-minutes:120}") long ttlMinutes) {
        return new JwtService(secret, Duration.ofMinutes(ttlMinutes));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
