package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.exception.UnsupportedDisasterTypeException;
import com.afet.monitoring.domain.model.DisasterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Factory Pattern — the factory holds the full handler list and asks each whether it
 * supports the type, so there is no switch to drift. Verifies every registered type
 * resolves to its own handler, and an unregistered type fails loudly.
 */
@DisplayName("DisasterHandlerFactory resolves handlers without a switch")
class DisasterHandlerFactoryTest {

    private final DisasterHandlerFactory factory = new DisasterHandlerFactory(List.of(
            new EarthquakeDisasterHandler(),
            new FloodDisasterHandler(),
            new WildfireDisasterHandler()));

    @ParameterizedTest(name = "create({0}) -> handler for {0}")
    @EnumSource(DisasterType.class)
    void returns_handler_matching_type(DisasterType type) {
        DisasterHandler handler = factory.create(type);

        assertThat(handler.type()).isEqualTo(type);
        assertThat(handler.supports(type)).isTrue();
    }

    @Test
    @DisplayName("throws when no handler is registered for the type")
    void throws_for_unregistered_type() {
        DisasterHandlerFactory onlyEarthquake =
                new DisasterHandlerFactory(List.of(new EarthquakeDisasterHandler()));

        assertThatThrownBy(() -> onlyEarthquake.create(DisasterType.FLOOD))
                .isInstanceOf(UnsupportedDisasterTypeException.class);
    }
}
