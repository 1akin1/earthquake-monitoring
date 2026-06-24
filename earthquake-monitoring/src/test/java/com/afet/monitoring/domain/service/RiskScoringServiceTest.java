package com.afet.monitoring.domain.service;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.RiskScore;
import com.afet.monitoring.domain.service.strategy.CriticalRiskStrategy;
import com.afet.monitoring.domain.service.strategy.HighRiskStrategy;
import com.afet.monitoring.domain.service.strategy.LowRiskStrategy;
import com.afet.monitoring.domain.service.strategy.MediumRiskStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Strategy Pattern — verifies the service routes each magnitude to the one strategy
 * whose band covers it, including the exact band boundaries (3.99 vs 4.0, etc).
 */
@DisplayName("RiskScoringService selects the right strategy by magnitude band")
class RiskScoringServiceTest {

    private final RiskScoringService service = new RiskScoringService(List.of(
            new LowRiskStrategy(),
            new MediumRiskStrategy(),
            new HighRiskStrategy(),
            new CriticalRiskStrategy()));

    private static Earthquake quake(double magnitude, double depthKm) {
        return Earthquake.register(
                new Magnitude(magnitude), depthKm,
                new GeoLocation(40.65, 29.27), "TEST",
                Instant.parse("2026-06-21T10:00:00Z"));
    }

    @ParameterizedTest(name = "magnitude {0} -> {1}")
    @CsvSource({
            "0.0,  LOW",
            "3.99, LOW",
            "4.0,  MEDIUM",   // lower boundary of MEDIUM
            "5.99, MEDIUM",
            "6.0,  HIGH",     // lower boundary of HIGH
            "7.99, HIGH",
            "8.0,  CRITICAL", // lower boundary of CRITICAL
            "10.0, CRITICAL"
    })
    void picks_strategy_by_band(double magnitude, RiskLevel expected) {
        RiskScore score = service.assess(quake(magnitude, 10.0));

        assertThat(score.level()).isEqualTo(expected);
        assertThat(score.value()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("fails fast when no registered strategy covers the magnitude")
    void throws_when_no_strategy_matches() {
        RiskScoringService onlyLow = new RiskScoringService(List.of(new LowRiskStrategy()));

        assertThatThrownBy(() -> onlyLow.assess(quake(8.5, 10.0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No risk strategy");
    }
}
