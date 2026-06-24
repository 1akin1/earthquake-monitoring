package com.afet.monitoring.domain.exception;

public class EarthquakeNotFoundException extends RuntimeException {
    public EarthquakeNotFoundException(Long id) {
        super("Earthquake not found with id: " + id);
    }
}
