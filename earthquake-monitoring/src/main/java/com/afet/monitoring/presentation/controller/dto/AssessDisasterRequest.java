package com.afet.monitoring.presentation.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Inbound DTO. {@code type} is a raw string ("EARTHQUAKE" / "FLOOD" / "WILDFIRE") —
 * parsed into the {@code DisasterType} enum at the controller edge.
 */
public record AssessDisasterRequest(
        @NotBlank String type,
        @NotNull @PositiveOrZero Double intensity) {}
