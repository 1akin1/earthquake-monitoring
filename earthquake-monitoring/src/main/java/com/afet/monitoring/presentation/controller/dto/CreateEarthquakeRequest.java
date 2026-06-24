package com.afet.monitoring.presentation.controller.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

/** Inbound DTO. Bean Validation guards the edges before anything reaches the domain. */
public record CreateEarthquakeRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double magnitude,
        @NotNull @PositiveOrZero Double depthKm,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank String source,
        @NotNull Instant occurredAt) {}
