package com.afet.monitoring.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads a {@code Authorization: Bearer <jwt>} header, validates it with {@link JwtService},
 * and, on success, populates the {@link SecurityContextHolder} with the user's single
 * {@code ROLE_*} authority. Runs once per request, before the username/password filter.
 *
 * <p>On a missing/invalid/expired token it simply leaves the context unauthenticated and
 * continues the chain — the authorization rules then decide the outcome (401/403). The
 * filter never writes the response itself, keeping the policy in one place.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            try {
                JwtService.Claims claims = jwtService.parse(header.substring(BEARER.length()));
                var authorities = List.of(new SimpleGrantedAuthority(claims.role().authority()));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.subject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ex) {
                SecurityContextHolder.clearContext(); // invalid token -> stay anonymous
            }
        }
        filterChain.doFilter(request, response);
    }
}
