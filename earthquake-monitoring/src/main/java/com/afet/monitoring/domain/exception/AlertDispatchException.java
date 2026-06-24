package com.afet.monitoring.domain.exception;

/**
 * Raised by the {@code AlertDispatcher} when an alert command still fails after all retry
 * attempts are exhausted. Carries the command description and the attempt count, and keeps
 * the last underlying failure as the cause.
 */
public class AlertDispatchException extends RuntimeException {

    public AlertDispatchException(String description, int attempts, Throwable cause) {
        super("Alert command failed after " + attempts + " attempt(s): " + description, cause);
    }
}
