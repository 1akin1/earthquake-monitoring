package com.afet.monitoring.domain.exception;

/** Raised when no handler exists for the requested disaster type. */
public class UnsupportedDisasterTypeException extends RuntimeException {
    public UnsupportedDisasterTypeException(String requestedType) {
        super("Unsupported disaster type: " + requestedType);
    }
}
