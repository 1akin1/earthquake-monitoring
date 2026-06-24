package com.afet.monitoring.domain.service.detection;

import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.SeismicSignal;
import java.time.Duration;
import java.time.Instant;

/**
 * STA/LTA detector — the classic seismological trigger. It slides two windows over the
 * signal's absolute amplitude: a SHORT-term average (recent energy) and a LONG-term
 * average (background noise). When STA/LTA rises above {@code triggerRatio}, a sudden
 * burst of energy has arrived → an earthquake is detected at that sample.
 *
 * <p>Pure domain, no framework. Window sizes and the threshold are injected so the
 * detector is tunable (see {@code DetectionConfig}).
 */
public class StaLtaDetector implements EarthquakeDetector {

    private final int staWindow;       // samples
    private final int ltaWindow;       // samples (>> staWindow)
    private final double triggerRatio; // STA/LTA threshold to fire

    public StaLtaDetector(int staWindow, int ltaWindow, double triggerRatio) {
        if (staWindow <= 0 || ltaWindow <= staWindow) {
            throw new IllegalArgumentException("require 0 < staWindow < ltaWindow");
        }
        this.staWindow = staWindow;
        this.ltaWindow = ltaWindow;
        this.triggerRatio = triggerRatio;
    }

    @Override
    public DetectionResult analyze(SeismicSignal signal) {
        double[] x = signal.amplitudes();
        int n = x.length;
        double peak = 0.0;
        for (double v : x) {
            peak = Math.max(peak, Math.abs(v));
        }

        if (n < ltaWindow + 1) {
            return DetectionResult.notDetected(peak, 0.0); // not enough history for LTA
        }

        double maxRatio = 0.0;
        for (int i = ltaWindow; i < n; i++) {
            double sta = mean(x, i - staWindow + 1, i);
            double lta = mean(x, i - ltaWindow + 1, i);
            double ratio = lta == 0.0 ? (sta > 0 ? Double.MAX_VALUE : 0.0) : sta / lta;
            maxRatio = Math.max(maxRatio, ratio);

            if (ratio >= triggerRatio) {
                Instant triggeredAt = signal.startTime()
                        .plus(Duration.ofNanos((long) (i / signal.sampleRateHz() * 1_000_000_000L)));
                return DetectionResult.detected(estimateMagnitude(peak), peak, ratio, triggeredAt);
            }
        }
        return DetectionResult.notDetected(peak, maxRatio);
    }

    /** Mean of |amplitude| over the inclusive index range [from, to]. */
    private double mean(double[] x, int from, int to) {
        double sum = 0.0;
        for (int i = from; i <= to; i++) {
            sum += Math.abs(x[i]);
        }
        return sum / (to - from + 1);
    }

    /**
     * Simplified magnitude estimate from peak amplitude: {@code M = log10(peak) + 3.0},
     * clamped to [0, 10]. A real estimate needs station distance and instrument
     * calibration; this is a deliberately simple placeholder.
     */
    private double estimateMagnitude(double peak) {
        if (peak <= 0) return 0.0;
        double m = Math.log10(peak) + 3.0;
        return Math.max(0.0, Math.min(10.0, m));
    }
}
