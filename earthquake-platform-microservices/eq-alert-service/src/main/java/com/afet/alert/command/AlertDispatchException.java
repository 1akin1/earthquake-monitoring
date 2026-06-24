package com.afet.alert.command;

/** Raised when an alert command still fails after all retry attempts. */
public class AlertDispatchException extends RuntimeException {
    public AlertDispatchException(String description, int attempts, Throwable cause) {
        super("Alert command failed after " + attempts + " attempt(s): " + description, cause);
    }
}
