package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.infrastructure.security.DemoUserStore;
import com.afet.monitoring.infrastructure.security.JwtService;
import com.afet.monitoring.infrastructure.security.Role;
import com.afet.monitoring.presentation.controller.dto.LoginRequest;
import com.afet.monitoring.presentation.controller.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues JWTs. {@code POST /api/auth/login} checks credentials against the demo user store
 * and, on success, returns a signed token carrying the user's role. This endpoint is open;
 * everything else is gated by the token it hands out.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DemoUserStore users;
    private final JwtService jwtService;

    public AuthController(DemoUserStore users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Role role = users.authenticate(request.username(), request.password())
                .orElseThrow(() -> new BadCredentialsException("invalid username or password"));

        String token = jwtService.issue(request.username(), role);
        JwtService.Claims claims = jwtService.parse(token);
        return TokenResponse.bearer(token, role.name(), claims.expiresAt());
    }
}
