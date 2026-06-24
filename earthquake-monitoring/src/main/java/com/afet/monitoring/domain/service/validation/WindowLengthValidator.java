package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Link 2 — analysability. The STA/LTA detector needs at least {@code ltaWindow + 1}
 * samples before it can compute a background average; a shorter window can never trigger
 * and would be silently reported as "no earthquake". Rejecting it here turns that silent
 * non-result into an explicit, honest error.
 *
 * <p>{@code minSamples} is injected so it can track the detector's configured LTA window
 * (see {@code SignalValidationConfig}).
 */
public class WindowLengthValidator extends SignalValidator {

    private final int minSamples;

    public WindowLengthValidator(int minSamples) {
        if (minSamples < 1) {
            throw new IllegalArgumentException("minSamples must be >= 1");
        }
        this.minSamples = minSamples;
    }

    @Override
    protected void check(SeismicSignal signal) {
        int n = signal.length();
        if (n < minSamples) {
            reject("only " + n + " samples; the detector needs at least " + minSamples);
        }
    }
}
