package com.afet.monitoring.domain.service.strategy;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.model.RiskLevel;
import com.afet.monitoring.domain.model.RiskScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Each strategy owns its own formula. These lock in the exact numbers so a formula
 * change can't pass silently, and assert the structural property that shallower quakes
 * score higher than deep ones at the same magnitude.
 */
@DisplayName("Risk strategy formulas")
class RiskStrategyFormulaTest {

    private static Earthquake quake(double magnitude, double depthKm) {
        return Earthquake.register(
                new Magnitude(magnitude), depthKm,
                new GeoLocation(0, 0), "TEST", Instant.EPOCH);
    }

    @Test
    @DisplayName("LOW = magnitude * 5")
    void low_formula() {
        RiskScore s = new LowRiskStrategy().score(quake(3.0, 10.0));
        assertThat(s.level()).isEqualTo(RiskLevel.LOW);
        assertThat(s.value()).isCloseTo(15.0, within(1e-9)); // 3.0 * 5
    }

    @Test
    @DisplayName("MEDIUM = 20 + mag*5 + depthFactor*10")
    void medium_formula() {
        RiskScore s = new MediumRiskStrategy().score(quake(5.0, 0.0));
        assertThat(s.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(s.value()).isCloseTo(55.0, within(1e-9)); // 20 + 25 + 10
    }

    @Test
    @DisplayName("HIGH = 50 + (mag-6)*10 + depthFactor*15")
    void high_formula() {
        RiskScore s = new HighRiskStrategy().score(quake(6.0, 0.0));
        assertThat(s.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(s.value()).isCloseTo(65.0, within(1e-9)); // 50 + 0 + 15
    }

    @Test
    @DisplayName("CRITICAL clamps at 100")
    void critical_clamps() {
        RiskScore s = new CriticalRiskStrategy().score(quake(10.0, 0.0));
        assertThat(s.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(s.value()).isCloseTo(100.0, within(1e-9)); // 85 + 10 + 10 = 105 -> 100
    }

    @Test
    @DisplayName("shallow quakes score higher than deep ones at equal magnitude")
    void depth_lowers_score() {
        double shallow = new HighRiskStrategy().score(quake(7.0, 0.0)).value();
        double deep    = new HighRiskStrategy().score(quake(7.0, 100.0)).value();
        assertThat(shallow).isGreaterThan(deep);
    }
}
