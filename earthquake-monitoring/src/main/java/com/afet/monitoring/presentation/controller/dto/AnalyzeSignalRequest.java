package com.afet.monitoring.presentation.controller.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

/** Inbound DTO: a raw signal window to analyse for an earthquake. */
public record AnalyzeSignalRequest(
        @NotBlank String stationId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @Positive Double sampleRateHz,
        @NotNull Instant startTime,
        @NotEmpty double[] amplitudes) {}
