package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.model.SeismicSignal;

/**
 * Link 4 — saturation. When ground motion exceeds the instrument's range the trace
 * "clips": many samples pile up at the exact same peak value (flat-topping at the rail).
 * A clipped trace yields an unreliable magnitude, so it is rejected when the fraction of
 * samples sitting exactly at the peak amplitude exceeds {@code maxPeakFraction}.
 *
 * <p>Scale-free: it compares against the signal's own peak, not an absolute voltage, so
 * it works regardless of the station's gain. Runs last — it is the most situational rule.
 */
public class ClippingValidator extends SignalValidator {

    private final double maxPeakFraction;

    public ClippingValidator(double maxPeakFraction) {
        if (maxPeakFraction <= 0 || maxPeakFraction > 1) {
            throw new IllegalArgumentException("maxPeakFraction must be in (0, 1]");
        }
        this.maxPeakFraction = maxPeakFraction;
    }

    @Override
    protected void check(SeismicSignal signal) {
        double[] x = signal.amplitudes();
        int n = x.length;

        double peak = 0.0;
        for (double v : x) {
            peak = Math.max(peak, Math.abs(v));
        }
        if (peak == 0.0) {
            return; // a flat-zero trace is a dead channel, not clipping — let that link own it
        }

        int atPeak = 0;
        for (double v : x) {
            if (Math.abs(v) == peak) {
                atPeak++;
            }
        }

        double fraction = (double) atPeak / n;
        if (atPeak > 1 && fraction > maxPeakFraction) {
            long pct = Math.round(fraction * 100.0);
            reject("appears clipped: " + pct + "% of samples sit at the peak amplitude");
        }
    }
}
