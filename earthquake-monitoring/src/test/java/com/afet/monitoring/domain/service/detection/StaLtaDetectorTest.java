package com.afet.monitoring.domain.service.detection;

import com.afet.monitoring.domain.model.DetectionResult;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.SeismicSignal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * STA/LTA trigger. Signals are built deterministically so the detector's output —
 * whether it fires, at which sample, and the estimated magnitude — is exactly known.
 */
@DisplayName("StaLtaDetector fires on a sudden burst, stays quiet on noise")
class StaLtaDetectorTest {

    private static final Instant T0 = Instant.parse("2026-06-21T10:00:00Z");

    // STA = 10 samples, LTA = 100 samples, fire when short-term energy is 4x background.
    private final EarthquakeDetector detector = new StaLtaDetector(10, 100, 4.0);

    private static SeismicSignal signal(double[] amplitudes) {
        return new SeismicSignal("STA-YLV", new GeoLocation(40.65, 29.27), 100.0, T0, amplitudes);
    }

    @Test
    @DisplayName("constructor rejects invalid window sizes")
    void rejects_bad_windows() {
        assertThatThrownBy(() -> new StaLtaDetector(0, 100, 4.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StaLtaDetector(10, 10, 4.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StaLtaDetector(10, 5, 4.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a window shorter than the LTA history cannot be detected")
    void too_short_is_not_detected() {
        double[] x = new double[50]; // < ltaWindow + 1 (= 101)
        Arrays.fill(x, 1.0);

        DetectionResult r = detector.analyze(signal(x));

        assertThat(r.detected()).isFalse();
        assertThat(r.estimatedMagnitude()).isNull();
        assertThat(r.triggeredAt()).isNull();
    }

    @Test
    @DisplayName("flat background never trips the trigger (ratio stays at 1.0)")
    void noise_is_not_detected() {
        double[] x = new double[200];
        Arrays.fill(x, 1.0); // constant -> STA/LTA == 1.0 at every sample

        DetectionResult r = detector.analyze(signal(x));

        assertThat(r.detected()).isFalse();
        assertThat(r.staLtaRatio()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("a sustained burst over quiet background is detected at burst onset")
    void burst_is_detected() {
        double[] x = new double[200];
        Arrays.fill(x, 0, 150, 1.0);     // samples 0..149: background
        Arrays.fill(x, 150, 200, 100.0); // samples 150..199: burst

        DetectionResult r = detector.analyze(signal(x));

        assertThat(r.detected()).isTrue();
        assertThat(r.peakAmplitude()).isCloseTo(100.0, within(1e-9));
        assertThat(r.staLtaRatio()).isGreaterThanOrEqualTo(4.0);
        // M = log10(peak=100) + 3 = 5.0
        assertThat(r.estimatedMagnitude()).isCloseTo(5.0, within(1e-9));
        // first trigger at sample 150 -> 150 / 100 Hz = 1.5 s after the first sample
        assertThat(r.triggeredAt()).isEqualTo(T0.plusMillis(1500));
    }
}
