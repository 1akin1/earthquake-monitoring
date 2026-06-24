package com.afet.monitoring.infrastructure.feed.decorator;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.model.GeoLocation;
import com.afet.monitoring.domain.model.Magnitude;
import com.afet.monitoring.domain.port.SeismicFeedPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decorator Pattern — each layer adds one transformation around a wrapped feed while
 * keeping the {@link SeismicFeedPort} interface, and the layers compose into a stack.
 */
@DisplayName("Feed decorators condition the raw event stream and stack transparently")
class SeismicFeedDecoratorTest {

    private static Earthquake quake(double magnitude, double depthKm, Instant when, double lat, double lon) {
        return Earthquake.register(new Magnitude(magnitude), depthKm,
                new GeoLocation(lat, lon), "TEST", when);
    }

    /** A feed that returns whatever fixed list it is given. */
    private static SeismicFeedPort feedOf(List<Earthquake> events) {
        return new SeismicFeedPort() {
            @Override public String sourceName() { return "FAKE"; }
            @Override public List<Earthquake> fetchRecent() { return events; }
        };
    }

    @Test
    @DisplayName("min-magnitude layer drops sub-threshold events, keeps the rest")
    void min_magnitude_filters() {
        SeismicFeedPort feed = new MinMagnitudeFeedDecorator(feedOf(List.of(
                quake(1.2, 10, Instant.parse("2026-06-20T00:00:00Z"), 40, 29),
                quake(2.5, 10, Instant.parse("2026-06-20T01:00:00Z"), 40, 29),
                quake(5.0, 10, Instant.parse("2026-06-20T02:00:00Z"), 40, 29))), 2.5);

        List<Earthquake> out = feed.fetchRecent();

        assertThat(out).hasSize(2);
        assertThat(out).allMatch(e -> e.magnitude().value() >= 2.5);
        assertThat(feed.sourceName()).isEqualTo("FAKE"); // identity preserved
    }

    @Test
    @DisplayName("depth-defaulting layer fills missing depth, leaves real depth untouched")
    void depth_defaulting_enriches() {
        SeismicFeedPort feed = new DepthDefaultingFeedDecorator(feedOf(List.of(
                quake(4.0, 0.0, Instant.parse("2026-06-20T00:00:00Z"), 40, 29),   // missing
                quake(4.0, 12.5, Instant.parse("2026-06-20T01:00:00Z"), 40, 29))), // real
                10.0);

        List<Earthquake> out = feed.fetchRecent();

        assertThat(out.get(0).depthKm()).isEqualTo(10.0); // enriched
        assertThat(out.get(1).depthKm()).isEqualTo(12.5); // untouched
    }

    @Test
    @DisplayName("dedup layer drops repeats of the same time/magnitude/location, keeps first")
    void dedup_collapses_repeats() {
        Instant t = Instant.parse("2026-06-20T00:00:00Z");
        SeismicFeedPort feed = new DeduplicatingFeedDecorator(feedOf(List.of(
                quake(5.0, 10, t, 40.000, 29.000),
                quake(5.0, 10, t, 40.0001, 29.0001), // same to 3 dp -> duplicate
                quake(5.0, 10, Instant.parse("2026-06-20T02:00:00Z"), 40, 29)))); // different time

        List<Earthquake> out = feed.fetchRecent();

        assertThat(out).hasSize(2);
    }

    @Test
    @DisplayName("the full stack applies filter, enrichment and dedup together")
    void full_stack_composes() {
        Instant t = Instant.parse("2026-06-20T00:00:00Z");
        SeismicFeedPort raw = feedOf(List.of(
                quake(1.0, 0, t, 40, 29),                                   // dropped (low mag)
                quake(4.0, 0, t, 41, 30),                                   // kept, depth enriched
                quake(4.0, 0, t, 41.0001, 30.0001),                         // duplicate of previous
                quake(6.0, 8, Instant.parse("2026-06-20T03:00:00Z"), 39, 28))); // kept as-is

        SeismicFeedPort stack = new DeduplicatingFeedDecorator(
                new DepthDefaultingFeedDecorator(
                        new MinMagnitudeFeedDecorator(raw, 2.5), 10.0));

        List<Earthquake> out = stack.fetchRecent();

        assertThat(out).hasSize(2);                       // low-mag dropped, duplicate collapsed
        assertThat(out.get(0).magnitude().value()).isEqualTo(4.0);
        assertThat(out.get(0).depthKm()).isEqualTo(10.0); // enriched
        assertThat(out.get(1).magnitude().value()).isEqualTo(6.0);
        assertThat(out.get(1).depthKm()).isEqualTo(8.0);  // untouched
    }
}
