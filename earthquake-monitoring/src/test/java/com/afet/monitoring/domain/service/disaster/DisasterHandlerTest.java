package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.model.DisasterAssessment;
import com.afet.monitoring.domain.model.DisasterType;
import com.afet.monitoring.domain.model.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each handler reads {@code intensity} in its own units and maps it to a shared
 * RiskLevel. These pin the band boundaries per disaster type — the whole reason the
 * Factory exists is that these thresholds differ.
 */
@DisplayName("Disaster handlers map type-specific intensity to a risk level")
class DisasterHandlerTest {

    @Nested
    @DisplayName("Earthquake — intensity is Richter magnitude")
    class Quake {
        private final EarthquakeDisasterHandler handler = new EarthquakeDisasterHandler();

        @ParameterizedTest(name = "magnitude {0} -> {1}")
        @CsvSource({"3.0,LOW", "4.0,MEDIUM", "5.9,MEDIUM", "6.0,HIGH", "7.9,HIGH", "8.0,CRITICAL", "9.5,CRITICAL"})
        void bands(double intensity, RiskLevel expected) {
            DisasterAssessment a = handler.assess(intensity);
            assertThat(a.type()).isEqualTo(DisasterType.EARTHQUAKE);
            assertThat(a.level()).isEqualTo(expected);
            assertThat(a.advisory()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Flood — intensity is metres above flood stage")
    class Flood {
        private final FloodDisasterHandler handler = new FloodDisasterHandler();

        @ParameterizedTest(name = "{0} m -> {1}")
        @CsvSource({"0.5,LOW", "1.0,MEDIUM", "2.9,MEDIUM", "3.0,HIGH", "4.9,HIGH", "5.0,CRITICAL", "8.0,CRITICAL"})
        void bands(double intensity, RiskLevel expected) {
            DisasterAssessment a = handler.assess(intensity);
            assertThat(a.type()).isEqualTo(DisasterType.FLOOD);
            assertThat(a.level()).isEqualTo(expected);
            assertThat(a.advisory()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Wildfire — intensity is burned area in km^2")
    class Wildfire {
        private final WildfireDisasterHandler handler = new WildfireDisasterHandler();

        @ParameterizedTest(name = "{0} km2 -> {1}")
        @CsvSource({"5,LOW", "10,MEDIUM", "99,MEDIUM", "100,HIGH", "499,HIGH", "500,CRITICAL", "1200,CRITICAL"})
        void bands(double intensity, RiskLevel expected) {
            DisasterAssessment a = handler.assess(intensity);
            assertThat(a.type()).isEqualTo(DisasterType.WILDFIRE);
            assertThat(a.level()).isEqualTo(expected);
            assertThat(a.advisory()).isNotBlank();
        }
    }
}
