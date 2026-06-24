package com.afet.monitoring.domain.service.validation;

import com.afet.monitoring.domain.exception.SignalRejectedException;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.SeismicSignal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chain of Responsibility — proves the chain visits links in order, passes the signal
 * along while they accept, and short-circuits at the first link that rejects. Then checks
 * each real validator accepts a good signal and rejects its specific defect.
 */
@DisplayName("SignalValidator chain runs links in order and stops at the first rejection")
class SignalValidatorChainTest {

    /** Build a signal with the given amplitudes; other fields are valid fixed values. */
    private static SeismicSignal signal(double[] amplitudes) {
        return new SeismicSignal("STA-YLV", new GeoLocation(40.0, 29.0),
                100.0, Instant.parse("2026-06-21T00:00:00Z"), amplitudes);
    }

    /** A clean, varied signal of 200 samples that every real validator should accept. */
    private static double[] healthySamples() {
        double[] x = new double[200];
        for (int i = 0; i < x.length; i++) {
            x[i] = Math.sin(i * 0.3) * (1 + (i % 7)); // varied, no clipping, finite
        }
        return x;
    }

    /** Records that it ran, and optionally rejects, so order/short-circuit can be asserted. */
    private static final class ProbeValidator extends SignalValidator {
        private final List<String> visited;
        private final String id;
        private final boolean rejectHere;

        ProbeValidator(List<String> visited, String id, boolean rejectHere) {
            this.visited = visited;
            this.id = id;
            this.rejectHere = rejectHere;
        }

        @Override
        protected void check(SeismicSignal signal) {
            visited.add(id);
            if (rejectHere) {
                reject("stop at " + id);
            }
        }
    }

    @Test
    @DisplayName("when every link accepts, all run in chain order")
    void all_links_run_in_order() {
        List<String> visited = new ArrayList<>();
        ProbeValidator a = new ProbeValidator(visited, "a", false);
        a.linkTo(new ProbeValidator(visited, "b", false))
                .linkTo(new ProbeValidator(visited, "c", false));

        a.validate(signal(healthySamples()));

        assertThat(visited).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("the first rejecting link short-circuits the chain — later links never run")
    void rejection_short_circuits() {
        List<String> visited = new ArrayList<>();
        ProbeValidator a = new ProbeValidator(visited, "a", false);
        a.linkTo(new ProbeValidator(visited, "b", true))   // rejects here
                .linkTo(new ProbeValidator(visited, "c", false));

        assertThatThrownBy(() -> a.validate(signal(healthySamples())))
                .isInstanceOf(SignalRejectedException.class)
                .hasMessageContaining("stop at b");

        assertThat(visited).containsExactly("a", "b"); // "c" never reached
    }

    @Test
    @DisplayName("FiniteAmplitudeValidator rejects NaN/Infinity, accepts finite data")
    void finite_validator() {
        FiniteAmplitudeValidator v = new FiniteAmplitudeValidator();

        assertThatThrownBy(() -> v.validate(signal(new double[]{1, 2, Double.NaN, 4})))
                .isInstanceOf(SignalRejectedException.class)
                .hasMessageContaining("non-finite");
        assertThatCode(() -> v.validate(signal(new double[]{1, 2, 3, 4})))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("WindowLengthValidator rejects too-short windows")
    void window_length_validator() {
        WindowLengthValidator v = new WindowLengthValidator(101);

        assertThatThrownBy(() -> v.validate(signal(new double[50])))
                .isInstanceOf(SignalRejectedException.class)
                .hasMessageContaining("at least 101");
        assertThatCode(() -> v.validate(signal(healthySamples()))) // 200 samples
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DeadChannelValidator rejects a flat trace, accepts a varied one")
    void dead_channel_validator() {
        DeadChannelValidator v = new DeadChannelValidator(1e-6);

        double[] flat = new double[200];
        java.util.Arrays.fill(flat, 5.0); // constant → std dev 0
        assertThatThrownBy(() -> v.validate(signal(flat)))
                .isInstanceOf(SignalRejectedException.class)
                .hasMessageContaining("flat trace");
        assertThatCode(() -> v.validate(signal(healthySamples())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ClippingValidator rejects a flat-topped trace, accepts a clean one")
    void clipping_validator() {
        ClippingValidator v = new ClippingValidator(0.20);

        // 200 samples, 100 of them pinned at the peak value 1000 → 50% clipped
        double[] clipped = new double[200];
        for (int i = 0; i < clipped.length; i++) {
            clipped[i] = (i % 2 == 0) ? 1000.0 : (i * 0.1);
        }
        assertThatThrownBy(() -> v.validate(signal(clipped)))
                .isInstanceOf(SignalRejectedException.class)
                .hasMessageContaining("clipped");
        assertThatCode(() -> v.validate(signal(healthySamples())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the assembled chain accepts a healthy signal end to end")
    void assembled_chain_accepts_healthy_signal() {
        SignalValidator head = new FiniteAmplitudeValidator();
        head.linkTo(new WindowLengthValidator(101))
                .linkTo(new DeadChannelValidator(1e-6))
                .linkTo(new ClippingValidator(0.20));

        assertThatCode(() -> head.validate(signal(healthySamples())))
                .doesNotThrowAnyException();
    }
}
