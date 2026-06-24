package com.afet.monitoring.domain.model;

/**
 * Value Object: an earthquake magnitude on the Richter-like scale.
 * Immutable, validated on construction. No identity — two magnitudes
 * with the same value are equal (record gives us that for free).
 */
public record Magnitude(double value) implements java.io.Serializable {
    public Magnitude {
        if (value < 0.0 || value > 10.0) {
            throw new IllegalArgumentException("Magnitude must be between 0 and 10, got: " + value);
        }
    }
}
