package com.afet.monitoring.infrastructure.config;

import com.afet.monitoring.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless, JWT-secured HTTP layer. The console is open (read-only) by default; a single
 *
 * ADMIN login unlocks the privileged operations.
 *
 * <p>Policy, evaluated top-down:
 * <ul>
 *   <li><b>open</b>: login, Swagger UI/spec, the health probe, the root redirect, and all
 *       reads (earthquakes, reports, stats, feed preview).</li>
 *   <li><b>ADMIN only</b>: detection, disaster assessment, a monitoring cycle, feed import,
 *       creating an earthquake, and deleting an earthquake.</li>
 * </ul>
 *
 * <p>Sessions are stateless (the JWT is the whole identity), CSRF is off (no cookies), and
 * the {@link JwtAuthenticationFilter} runs before the username/password filter.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/api/auth/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/actuator/health", "/actuator/info").permitAll()

                        // privileged operations require the single admin role
                        .requestMatchers(HttpMethod.DELETE, "/api/earthquakes/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/earthquakes/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/detection/**", "/api/disasters/**",
                                "/api/monitoring/**", "/api/feeds/import")
                        .hasRole("ADMIN")

                        // everything else (reads: earthquakes, reports, stats, feed preview) is open
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS for the browser SPA. Origins come from {@code app.cors.allowed-origins}
     * (comma-separated; overridable via the APP_CORS_ALLOWED_ORIGINS env). Credentials are
     * not used — the JWT travels in the Authorization header, not a cookie — so we allow
     * that header explicitly and keep {@code allowCredentials} off.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
