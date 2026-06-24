package com.afet.monitoring.infrastructure.security;

/** Raised when a JWT is malformed, has a bad signature, or has expired. */
public class JwtException extends RuntimeException {
    public JwtException(String message) {
        super(message);
    }
}
