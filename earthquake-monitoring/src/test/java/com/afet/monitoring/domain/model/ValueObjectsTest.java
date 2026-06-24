package com.afet.monitoring.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Value objects enforce their invariants in the constructor — an invalid one simply
 * cannot exist, so the rest of the domain never has to re-check.
 */
@DisplayName("Value objects validate on construction")
class ValueObjectsTest {

    @Nested
    @DisplayName("Magnitude (0..10)")
    class MagnitudeRules {
        @Test
        void accepts_bounds() {
            assertThat(new Magnitude(0.0).value()).isZero();
            assertThat(new Magnitude(10.0).value()).isEqualTo(10.0);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.1, 10.1, 99.0})
        void rejects_out_of_range(double bad) {
            assertThatThrownBy(() -> new Magnitude(bad)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void equal_by_value() {
            assertThat(new Magnitude(6.4)).isEqualTo(new Magnitude(6.4));
        }
    }

    @Nested
    @DisplayName("GeoLocation (lat -90..90, lon -180..180)")
    class GeoLocationRules {
        @Test
        void accepts_valid_point() {
            assertThatCode(() -> new GeoLocation(40.65, 29.27)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(doubles = {-91.0, 91.0})
        void rejects_bad_latitude(double lat) {
            assertThatThrownBy(() -> new GeoLocation(lat, 0)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-181.0, 181.0})
        void rejects_bad_longitude(double lon) {
            assertThatThrownBy(() -> new GeoLocation(0, lon)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("RiskScore (0..100 + level)")
    class RiskScoreRules {
        @ParameterizedTest
        @ValueSource(doubles = {-1.0, 100.01})
        void rejects_out_of_range(double bad) {
            assertThatThrownBy(() -> new RiskScore(bad, RiskLevel.LOW)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void requires_level() {
            assertThatThrownBy(() -> new RiskScore(50.0, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("SeismicSignal input guards")
    class SeismicSignalRules {
        @Test
        void rejects_blank_station() {
            assertThatThrownBy(() -> new SeismicSignal(" ", new GeoLocation(0, 0), 100, Instant.EPOCH, new double[]{1.0}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_empty_amplitudes() {
            assertThatThrownBy(() -> new SeismicSignal("STA", new GeoLocation(0, 0), 100, Instant.EPOCH, new double[]{}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_non_positive_sample_rate() {
            assertThatThrownBy(() -> new SeismicSignal("STA", new GeoLocation(0, 0), 0, Instant.EPOCH, new double[]{1.0}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
