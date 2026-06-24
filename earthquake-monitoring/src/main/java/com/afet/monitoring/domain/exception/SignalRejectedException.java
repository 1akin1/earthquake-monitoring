package com.afet.monitoring.domain.exception;

/**
 * Raised by a link in the signal-validation chain when a raw {@code SeismicSignal} is
 * structurally valid (well-formed request) but not analysable — e.g. too short for the
 * detector's window, a flat dead channel, clipped, or containing non-finite samples.
 *
 * <p>Carries which validator rejected it and why, so the API can return a precise reason.
 */
public class SignalRejectedException extends RuntimeException {

    private final String validator;

    public SignalRejectedException(String validator, String reason) {
        super("Signal rejected by " + validator + ": " + reason);
        this.validator = validator;
    }

    public String validator() {
        return validator;
    }
}
