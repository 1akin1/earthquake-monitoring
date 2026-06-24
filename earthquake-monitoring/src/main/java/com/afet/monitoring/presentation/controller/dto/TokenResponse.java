package com.afet.monitoring.presentation.controller.dto;

import java.time.Instant;

/** The issued JWT plus its role and expiry, returned from a successful login. */
public record TokenResponse(String token, String tokenType, String role, Instant expiresAt) {

    public static TokenResponse bearer(String token, String role, Instant expiresAt) {
        return new TokenResponse(token, "Bearer", role, expiresAt);
    }
}
