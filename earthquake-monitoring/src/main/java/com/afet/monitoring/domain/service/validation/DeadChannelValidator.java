package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Link 3 — sensor health. A working seismometer always shows some background micro-noise,
 * so a perfectly (or near-perfectly) flat trace means a dead or disconnected channel, not
 * a quiet earth. Rejects when the sample standard deviation falls below {@code minStdDev}.
 *
 * <p>The threshold is a small absolute value: only genuinely flat traces are rejected, so
 * legitimately quiet low-amplitude signals still pass.
 */
public class DeadChannelValidator extends SignalValidator {

    private final double minStdDev;

    public DeadChannelValidator(double minStdDev) {
        if (minStdDev < 0) {
            throw new IllegalArgumentException("minStdDev must be >= 0");
        }
        this.minStdDev = minStdDev;
    }

    @Override
    protected void check(SeismicSignal signal) {
        double[] x = signal.amplitudes();
        int n = x.length;

        double sum = 0.0;
        for (double v : x) {
            sum += v;
        }
        double mean = sum / n;

        double sqDiff = 0.0;
        for (double v : x) {
            double d = v - mean;
            sqDiff += d * d;
        }
        double stdDev = Math.sqrt(sqDiff / n);

        if (stdDev < minStdDev) {
            reject("flat trace (std dev " + stdDev + " < " + minStdDev
                    + "): likely a dead or disconnected channel");
        }
    }
}
