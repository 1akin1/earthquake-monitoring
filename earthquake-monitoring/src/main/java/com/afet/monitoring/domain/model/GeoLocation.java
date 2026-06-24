package com.afet.monitoring.domain.model;

/** Value Object: a point on Earth. Validated on construction. */
public record GeoLocation(double latitude, double longitude) implements java.io.Serializable {
    public GeoLocation {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude out of range: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude out of range: " + longitude);
        }
    }
}
