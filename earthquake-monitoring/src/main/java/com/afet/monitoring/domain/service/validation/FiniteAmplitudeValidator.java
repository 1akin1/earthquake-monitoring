package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Link 1 — data integrity. Rejects a signal containing any {@code NaN} or infinite
 * sample; such values would poison every downstream average in the STA/LTA detector.
 * The cheapest, most fundamental check, so it runs first.
 */
public class FiniteAmplitudeValidator extends SignalValidator {

    @Override
    protected void check(SeismicSignal signal) {
        double[] x = signal.amplitudes();
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i])) {
                reject("non-finite sample at index " + i + " (NaN or Infinity)");
            }
        }
    }
}
