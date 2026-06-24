package com.afet.monitoring.infrastructure.feed.decorator;

import com.afet.monitoring.domain.model.Earthquake;
import com.afet.monitoring.domain.port.SeismicFeedPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator — enrichment. Some feeds report no usable depth (a zero or negative value
 * where the instrument couldn't resolve it). Rather than let a meaningless 0 km flow
 * downstream, this layer substitutes a sensible regional default, rebuilding the event as
 * a new immutable {@link Earthquake}. Events that already carry a real depth pass through
 * untouched.
 */
public class DepthDefaultingFeedDecorator extends SeismicFeedDecorator {

    private final double defaultDepthKm;

    public DepthDefaultingFeedDecorator(SeismicFeedPort delegate, double defaultDepthKm) {
        super(delegate);
        if (defaultDepthKm <= 0) {
            throw new IllegalArgumentException("defaultDepthKm must be positive");
        }
        this.defaultDepthKm = defaultDepthKm;
    }

    @Override
    public List<Earthquake> fetchRecent() {
        List<Earthquake> enriched = new ArrayList<>();
        for (Earthquake e : delegate.fetchRecent()) {
            if (e.depthKm() <= 0.0) {
                enriched.add(Earthquake.register(
                        e.magnitude(), defaultDepthKm, e.location(), e.source(), e.occurredAt()));
            } else {
                enriched.add(e);
            }
        }
        return List.copyOf(enriched);
    }
}
