package com.afet.monitoring.domain.model;

import java.util.Objects;

/** Value Object: a 0–100 risk score plus its qualitative level. */
public record RiskScore(double value, RiskLevel level) implements java.io.Serializable {
    public RiskScore {
        if (value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100, got: " + value);
        }
        Objects.requireNonNull(level, "level");
    }
}
