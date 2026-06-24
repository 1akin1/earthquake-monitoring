package com.afet.monitoring.domain.model;

import com.afet.monitoring.domain.exception.UnsupportedDisasterTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The raw client string is parsed to an enum once, at the edge. Anything unknown,
 * blank or null is rejected before it can reach the business logic.
 */
@DisplayName("DisasterType.from parses the edge string safely")
class DisasterTypeTest {

    @Test
    void parses_case_insensitively_and_trims() {
        assertThat(DisasterType.from("  earthquake ")).isEqualTo(DisasterType.EARTHQUAKE);
        assertThat(DisasterType.from("FLOOD")).isEqualTo(DisasterType.FLOOD);
        assertThat(DisasterType.from("WildFire")).isEqualTo(DisasterType.WILDFIRE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"tsunami", "quake", " "})
    void rejects_unknown_or_blank(String raw) {
        assertThatThrownBy(() -> DisasterType.from(raw)).isInstanceOf(UnsupportedDisasterTypeException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> DisasterType.from(null)).isInstanceOf(UnsupportedDisasterTypeException.class);
    }
}
