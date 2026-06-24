package com.afet.monitoring.domain.model;

import java.util.Objects;

/**
 * Value Object: the result of letting a disaster-type handler evaluate a reading.
 * Carries the qualitative {@link RiskLevel} plus a short, type-specific advisory.
 * Reuses the same RiskLevel scale as earthquake risk scoring so the whole platform
 * speaks one severity language.
 */
public record DisasterAssessment(DisasterType type, RiskLevel level, String advisory) {
    public DisasterAssessment {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(advisory, "advisory");
    }
}
