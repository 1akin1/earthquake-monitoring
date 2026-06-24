package com.afet.monitoring.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Inbound credentials for {@code POST /api/auth/login}. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {}
